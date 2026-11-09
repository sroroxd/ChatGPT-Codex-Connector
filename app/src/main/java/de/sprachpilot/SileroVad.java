package de.sprachpilot;

import ai.onnxruntime.*;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.Map;

/** Silero v6: recurrent state [2,1,128], 64-sample context, 512-sample frames. */
final class SileroVad implements AutoCloseable {
    private final OrtEnvironment env = OrtEnvironment.getEnvironment();
    private final OrtSession session;
    private float[][][] state = new float[2][1][128];
    private final float[] context = new float[64];
    SileroVad(byte[] model) throws OrtException {
        env.setTelemetry(false);
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(1); options.setInterOpNumThreads(1);
            session = env.createSession(model, options);
        }
    }
    float probability(short[] pcm) throws OrtException {
        float[] input = new float[576];
        System.arraycopy(context, 0, input, 0, 64);
        for (int i = 0; i < 512; i++) input[i + 64] = pcm[i] / 32768f;
        System.arraycopy(input, 512, context, 0, 64);
        try (OnnxTensor x = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), new long[]{1,576});
             OnnxTensor s = OnnxTensor.createTensor(env, state);
             OnnxTensor sr = OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{16000}), new long[]{});
             OrtSession.Result out = session.run(Map.of("input",x,"state",s,"sr",sr))) {
            float result = ((float[][])out.get("output").orElseThrow().getValue())[0][0];
            state = (float[][][])out.get("stateN").orElseThrow().getValue();
            return result;
        }
    }
    @Override public void close() throws OrtException { session.close(); }
}
