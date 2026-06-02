package com.soundvisualization.accessibility

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sqrt

object SoundFeatureExtractor {
    const val LIVE_SPECTRUM_BAND_COUNT = 9

    private const val EPSILON = 0.000001
    private const val LIVE_SPECTRUM_GAIN = 22000.0
    private val BAND_FREQUENCIES = floatArrayOf(250f, 500f, 1000f, 2000f, 4000f)
    private val EXTRA_FINGERPRINT_FREQUENCIES = floatArrayOf(63f, 125f, 6000f, 7200f)
    private val LIVE_SPECTRUM_BANDS = arrayOf(
        SpectrumBand(45f, 63f, 90f),
        SpectrumBand(90f, 125f, 180f),
        SpectrumBand(180f, 250f, 355f),
        SpectrumBand(355f, 500f, 710f),
        SpectrumBand(710f, 1000f, 1420f),
        SpectrumBand(1420f, 2000f, 2840f),
        SpectrumBand(2840f, 4000f, 5680f),
        SpectrumBand(5680f, 8000f, 11300f),
        SpectrumBand(11300f, 16000f, 20000f),
    )

    fun liveSpectrum(
        samples: FloatArray,
        sampleCount: Int,
        sampleRate: Int,
        level: Float,
    ): FrequencyResponse {
        val count = sampleCount.coerceIn(0, samples.size)
        if (count <= 0) return FrequencyResponse(emptyList(), 0f)

        val output = ArrayList<Float>(LIVE_SPECTRUM_BAND_COUNT)
        var strongestEnergy = 0.0
        var dominantFrequency = 0f
        val countSquared = count.toDouble() * count.toDouble()
        val loudnessCurve = ln(1.0 + level.coerceIn(0f, 1f) * 5.0) / ln(6.0)
        val levelLift = (0.96 + loudnessCurve * 1.82).coerceIn(0.96, 2.78)

        for (band in LIVE_SPECTRUM_BANDS) {
            val energy =
                goertzelEnergy(samples, 0, count, sampleRate, band.lowFrequency) * 0.22 +
                    goertzelEnergy(samples, 0, count, sampleRate, band.centerFrequency) * 0.56 +
                    goertzelEnergy(samples, 0, count, sampleRate, band.highFrequency) * 0.22
            if (energy > strongestEnergy) {
                strongestEnergy = energy
                dominantFrequency = band.centerFrequency
            }
            val normalizedEnergy = energy / countSquared
            val response = ln(1.0 + normalizedEnergy * LIVE_SPECTRUM_GAIN) /
                ln(1.0 + LIVE_SPECTRUM_GAIN)
            output.add((response * levelLift).toFloat().coerceIn(0f, 1f))
        }

        return FrequencyResponse(output, dominantFrequency)
    }

    fun fingerprint(samples: FloatArray, sampleRate: Int): List<Float> {
        if (samples.isEmpty()) return emptyList()

        val segment = activeSegmentRange(samples)
        if (segment.size <= 0) return emptyList()

        var energy = 0.0
        var peak = 0f
        var zeroCrossings = 0
        var diffEnergy = 0.0
        var absTotal = 0.0
        var onsetCount = 0
        var previousAbs = abs(samples[segment.start])
        var weightedTime = 0.0

        for (index in segment.start until segment.endExclusive) {
            val value = samples[index]
            val localIndex = index - segment.start
            val absValue = abs(value)
            energy += value * value
            absTotal += absValue
            peak = max(peak, absValue)
            weightedTime += localIndex * absValue

            if (localIndex > 0) {
                val previous = samples[index - 1]
                if ((previous >= 0f && value < 0f) || (previous < 0f && value >= 0f)) {
                    zeroCrossings += 1
                }
                val diff = value - previous
                diffEnergy += diff * diff
                if (absValue > previousAbs * 1.9f && absValue > 0.025f) {
                    onsetCount += 1
                }
            }
            previousAbs = previousAbs * 0.86f + absValue * 0.14f
        }

        val count = segment.size.coerceAtLeast(1)
        val rms = sqrt(energy / count).toFloat().coerceIn(0f, 1f)
        val zcr = (zeroCrossings.toFloat() / count).coerceIn(0f, 1f)
        val meanAbs = (absTotal / count).toFloat().coerceIn(0f, 1f)
        val crest = (peak / (rms + EPSILON.toFloat()) / 14f).coerceIn(0f, 1f)
        val transient = sqrt(diffEnergy / count).toFloat()
            .let { (it / (meanAbs + EPSILON.toFloat()) / 8f).coerceIn(0f, 1f) }
        val onsetRate = (onsetCount.toFloat() / (count / sampleRate.toFloat()).coerceAtLeast(0.1f) / 24f)
            .coerceIn(0f, 1f)
        val temporalCentroid = (weightedTime / (absTotal + EPSILON) / count).toFloat().coerceIn(0f, 1f)

        val bands = DoubleArray(BAND_FREQUENCIES.size)
        var bandTotal = EPSILON
        for (index in BAND_FREQUENCIES.indices) {
            val energyValue = goertzelEnergy(
                samples = samples,
                start = segment.start,
                endExclusive = segment.endExclusive,
                sampleRate = sampleRate,
                frequency = BAND_FREQUENCIES[index],
            )
            bands[index] = energyValue
            bandTotal += energyValue
        }

        val extraBands = DoubleArray(EXTRA_FINGERPRINT_FREQUENCIES.size)
        var extraBandTotal = EPSILON
        for (index in EXTRA_FINGERPRINT_FREQUENCIES.indices) {
            val energyValue = goertzelEnergy(
                samples = samples,
                start = segment.start,
                endExclusive = segment.endExclusive,
                sampleRate = sampleRate,
                frequency = EXTRA_FINGERPRINT_FREQUENCIES[index],
            )
            extraBands[index] = energyValue
            extraBandTotal += energyValue
        }

        val temporalEnvelope = temporalEnvelope(samples, segment)

        val output = ArrayList<Float>(7 + BAND_FREQUENCIES.size + EXTRA_FINGERPRINT_FREQUENCIES.size + temporalEnvelope.size)
        output.add(rms)
        output.add(peak.coerceIn(0f, 1f))
        output.add(zcr)
        output.add(crest)
        output.add(transient)
        output.add(onsetRate)
        output.add(temporalCentroid)
        for (energyValue in bands) {
            output.add((ln(1.0 + energyValue / bandTotal * 100.0) / ln(101.0)).toFloat().coerceIn(0f, 1f))
        }
        for (energyValue in extraBands) {
            output.add((ln(1.0 + energyValue / extraBandTotal * 100.0) / ln(101.0)).toFloat().coerceIn(0f, 1f))
        }
        output.addAll(temporalEnvelope)
        return output
    }

    fun merge(existing: List<Float>, existingCount: Int, next: List<Float>): List<Float> {
        if (next.isEmpty()) return existing
        if (existing.size != next.size || existing.isEmpty() || existingCount <= 0) return next

        val weight = existingCount.coerceIn(1, 4).toFloat()
        val output = ArrayList<Float>(existing.size)
        for (index in existing.indices) {
            output.add(((existing[index] * weight + next[index]) / (weight + 1f)).coerceIn(0f, 1f))
        }
        return output
    }

    fun similarity(first: List<Float>, second: List<Float>): Float {
        val sharedSize = min(first.size, second.size)
        if (sharedSize <= 0) return 0f

        var dot = 0.0
        var firstNorm = 0.0
        var secondNorm = 0.0
        var distance = 0.0

        for (index in 0 until sharedSize) {
            val a = first[index].toDouble()
            val b = second[index].toDouble()
            dot += a * b
            firstNorm += a * a
            secondNorm += b * b
            distance += abs(a - b)
        }

        val cosine = dot / (sqrt(firstNorm) * sqrt(secondNorm) + EPSILON)
        val closeness = 1.0 - (distance / sharedSize).coerceIn(0.0, 1.0)
        return (cosine * 0.72 + closeness * 0.28).toFloat().coerceIn(0f, 1f)
    }

    private fun temporalEnvelope(samples: FloatArray, segment: SegmentRange): List<Float> {
        val count = segment.size.coerceAtLeast(1)
        val sliceCount = TEMPORAL_ENVELOPE_SLICES
        val output = ArrayList<Float>(sliceCount)
        var totalEnergy = EPSILON
        val sliceEnergy = DoubleArray(sliceCount)

        for (slice in 0 until sliceCount) {
            val start = segment.start + count * slice / sliceCount
            val end = segment.start + count * (slice + 1) / sliceCount
            var energy = 0.0
            for (index in start until end) {
                val sample = samples[index]
                energy += sample * sample
            }
            sliceEnergy[slice] = energy
            totalEnergy += energy
        }

        for (energy in sliceEnergy) {
            output.add((ln(1.0 + energy / totalEnergy * 24.0) / ln(25.0)).toFloat().coerceIn(0f, 1f))
        }
        return output
    }

    private fun activeSegmentRange(samples: FloatArray): SegmentRange {
        var peak = 0f
        for (sample in samples) {
            peak = max(peak, abs(sample))
        }
        if (peak <= 0f) return SegmentRange(0, samples.size)

        val threshold = max(0.012f, peak * 0.08f)
        var start = -1
        var end = -1
        for (index in samples.indices) {
            if (abs(samples[index]) >= threshold) {
                if (start < 0) start = index
                end = index
            }
        }
        if (start < 0 || end <= start) return SegmentRange(0, samples.size)

        val padding = (samples.size * 0.04f).toInt().coerceAtLeast(320)
        start = (start - padding).coerceAtLeast(0)
        end = (end + padding).coerceAtMost(samples.lastIndex)
        return SegmentRange(start, end + 1)
    }

    private fun goertzelEnergy(
        samples: FloatArray,
        start: Int,
        endExclusive: Int,
        sampleRate: Int,
        frequency: Float,
    ): Double {
        if (frequency <= 0f || frequency >= sampleRate * 0.5f) return 0.0

        val normalized = frequency / sampleRate
        val coefficient = 2.0 * cos(2.0 * PI * normalized)
        var q0: Double
        var q1 = 0.0
        var q2 = 0.0
        val segmentSize = (endExclusive - start).coerceAtLeast(1)
        val step = max(1, segmentSize / 8192)

        var index = start
        while (index < endExclusive) {
            q0 = coefficient * q1 - q2 + samples[index]
            q2 = q1
            q1 = q0
            index += step
        }

        return q1 * q1 + q2 * q2 - coefficient * q1 * q2
    }

    private data class SpectrumBand(
        val lowFrequency: Float,
        val centerFrequency: Float,
        val highFrequency: Float,
    )

    private data class SegmentRange(
        val start: Int,
        val endExclusive: Int,
    ) {
        val size: Int get() = endExclusive - start
    }

    data class FrequencyResponse(
        val bands: List<Float>,
        val dominantFrequencyHz: Float,
    )

    private const val TEMPORAL_ENVELOPE_SLICES = 4
}
