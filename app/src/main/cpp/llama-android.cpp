#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"
#include "common.h"
#include "sampling.h"

#define LOG_TAG "llama-android"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// JNI method signatures
JNIEXPORT jlong JNICALL Java_com_rescuemate_services_StreamingLLM_initModel(
    JNIEnv* env, jobject thiz, jstring modelPath) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) {
        LOGE("Failed to get model path");
        return 0;
    }

    // Initialize llama backend
    static bool is_initialized = false;
    if (!is_initialized) {
        llama_backend_init();
        is_initialized = true;
    }

    // Initialize llama model parameters
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only for Android

    struct llama_model * model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }

    // Initialize context parameters
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 512; // Context size
    ctx_params.n_threads = 4; // Threads
    ctx_params.n_threads_batch = 4;

    struct llama_context * ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_model_free(model);
        return 0;
    }

    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL Java_com_rescuemate_services_StreamingLLM_generateTokenStream(
    JNIEnv* env, jobject thiz, jlong contextPtr, jstring prompt, jobject callback) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (!ctx) {
        LOGE("Invalid context");
        return;
    }

    const struct llama_model * model = llama_get_model(ctx);

    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    if (!prompt_str) {
        LOGE("Failed to get prompt");
        return;
    }

    // Get callback method
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");

    // Tokenize prompt using common helper
    std::vector<llama_token> tokens = common_tokenize(ctx, prompt_str, true, true);
    env->ReleaseStringUTFChars(prompt, prompt_str);

    // Initialize sampler
    common_params_sampling params_sampling;
    struct common_sampler * smpl = common_sampler_init(model, params_sampling);

    // Create batch
    llama_batch batch = llama_batch_init(2048, 0, 1); // Max batch size

    // Evaluate prompt (prefill)
    LOGI("Evaluating prompt: %s", prompt_str);
    for (size_t i = 0; i < tokens.size(); i++) {
        common_batch_add(batch, tokens[i], i, { 0 }, false);
    }
    
    // Ensure at least one token is processed if prompt is empty (unlikely but safe)
    if (batch.n_tokens == 0) {
        common_batch_add(batch, llama_vocab_bos(llama_model_get_vocab(model)), 0, { 0 }, false);
    }

    // Set logits for the last token to sample from it
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(ctx, batch) != 0) {
        LOGE("Failed to decode prompt");
        llama_batch_free(batch);
        common_sampler_free(smpl);
        return;
    }
    
    LOGI("Prompt evaluated. Starting generation...");

    // Generation loop
    int n_cur = batch.n_tokens;
    const int n_ctx = llama_n_ctx(ctx);
    
    while (n_cur < n_ctx) {
        // Sample next token
        llama_token new_token_id = common_sampler_sample(smpl, ctx, -1);
        common_sampler_accept(smpl, new_token_id, true);

        // Check for EOS
        if (llama_vocab_is_eog(llama_model_get_vocab(model), new_token_id)) {
            LOGI("EOS token detected");
            break;
        }

        // Convert token to string and callback
        std::string piece = common_token_to_piece(ctx, new_token_id);
        
        // common_token_to_piece might return string with c_str() that is valid
        if (!piece.empty()) {
            LOGI("Generated token: %s", piece.c_str());
            jstring tokenStr = env->NewStringUTF(piece.c_str());
            env->CallObjectMethod(callback, onTokenMethod, tokenStr);
            env->DeleteLocalRef(tokenStr);
        } else {
             // LOGI("Generated empty token piece");
        }

        // Prepare next batch
        common_batch_clear(batch);
        common_batch_add(batch, new_token_id, n_cur, { 0 }, true);

        n_cur += 1;

        if (llama_decode(ctx, batch) != 0) {
            LOGE("Failed to decode token");
            break;
        }
    }

    llama_batch_free(batch);
    common_sampler_free(smpl);
}

JNIEXPORT void JNICALL Java_com_rescuemate_services_StreamingLLM_freeModel(
    JNIEnv* env, jobject thiz, jlong contextPtr) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx) {
        const struct llama_model * model = llama_get_model(ctx);
        llama_free(ctx);
        if (model) {
             llama_model_free(const_cast<struct llama_model *>(model));
        }
    }
}

}
