import {LatencyOptimisedTranslator, TranslatorBacking} from "./translator.js";

const REGISTRY_URL = "./models/registry.json";

function post(type, payload = {}) {
    if (globalThis.AndroidBridge?.postMessage) {
        AndroidBridge.postMessage(JSON.stringify({type, ...payload}));
    }
}

async function fetchArrayBuffer(path) {
    const response = await fetch(path, {cache: "force-cache"});
    if (!response.ok) {
        throw new Error(`Could not load ${path}: ${response.status}`);
    }
    return await response.arrayBuffer();
}

class BundledBergamotBacking extends TranslatorBacking {
    constructor(options) {
        const {onerror, ...cloneableOptions} = options || {};
        super({
            ...cloneableOptions,
            registryUrl: REGISTRY_URL,
            downloadTimeout: 0,
            pivotLanguage: "en",
        });
        if (onerror) {
            this.onerror = onerror;
        }
    }

    async loadModelRegistery() {
        const response = await fetch(REGISTRY_URL, {cache: "force-cache"});
        if (!response.ok) {
            throw new Error(`Could not load registry: ${response.status}`);
        }
        const registry = await response.json();
        return Object.values(registry);
    }

    async loadTranslationModel({from, to}) {
        const registry = await this.registry;
        const entry = registry.find(model => model.from === from && model.to === to);
        if (!entry) {
            throw new Error(`No bundled model for ${from}->${to}`);
        }

        const [model, shortlist] = await Promise.all([
            fetchArrayBuffer(entry.files.model),
            fetchArrayBuffer(entry.files.lex),
        ]);
        const vocabs = await Promise.all(entry.files.vocabs.map(fetchArrayBuffer));

        return {
            model,
            shortlist,
            vocabs,
            config: entry.config || {},
        };
    }
}

const backing = new BundledBergamotBacking({
    cacheSize: 64,
    downloadTimeout: 0,
    pivotLanguage: "en",
    useNativeIntGemm: false,
    onerror: error => post("engine-error", {message: error?.message || String(error)}),
});

const translator = new LatencyOptimisedTranslator({
    cacheSize: 64,
    downloadTimeout: 0,
    pivotLanguage: "en",
    useNativeIntGemm: false,
}, backing);

globalThis.SoundVizTranslate = async function translate(id, from, to, text) {
    try {
        if (from === to) {
            post("translation", {id, text});
            return;
        }

        const response = await translator.translate({
            from,
            to,
            text,
            html: false,
            qualityScores: false,
        });

        post("translation", {id, text: response?.target?.text || ""});
    } catch (error) {
        post("translation-error", {
            id,
            message: error?.message || String(error),
        });
    }
};

translator.worker
    .then(() => post("ready"))
    .catch(error => post("engine-error", {message: error?.message || String(error)}));
