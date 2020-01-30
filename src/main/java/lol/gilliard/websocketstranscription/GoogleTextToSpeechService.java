package lol.gilliard.websocketstranscription;

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client class for Google Cloud Speech-to-Text
 *
 * This class relies on having your Google Credentials in a file named by the
 * GOOGLE_APPLICATION_CREDENTIALS environment variable
 *
 * Based on: https://cloud.google.com/speech-to-text/docs/streaming-recognize#speech-streaming-recognize-java
 */
public class GoogleTextToSpeechService {

    final static Logger logger = LoggerFactory.getLogger(GoogleTextToSpeechService.class);
    ClientStream<StreamingRecognizeRequest> clientStream;
    ResponseObserver<StreamingRecognizeResponse> responseObserver;

    public GoogleTextToSpeechService(Consumer<String> onTranscription) throws IOException {
        SpeechClient client = SpeechClient.create();
        responseObserver = new ResponseObserver<StreamingRecognizeResponse>() {
            @Override
            public void onStart(StreamController streamController) {
                logger.info("Started");
            }

            @Override
            public void onResponse(StreamingRecognizeResponse streamingRecognizeResponse) {
                StreamingRecognitionResult result = streamingRecognizeResponse.getResultsList().get(0);
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                onTranscription.accept(alternative.getTranscript());
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error on recognize request: {}", throwable);
            }

            @Override
            public void onComplete() {
                logger.info("Completed");
            }
        };

        clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);

        RecognitionConfig recognitionConfig =
            RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.MULAW)
                .setLanguageCode("en-US")
                .setSampleRateHertz(8000)
                .build();
        StreamingRecognitionConfig streamingRecognitionConfig =
            StreamingRecognitionConfig.newBuilder()
                .setConfig(recognitionConfig)
                .setInterimResults(true)
                .build();

        StreamingRecognizeRequest request =
            StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(streamingRecognitionConfig)
                .build(); // The first request in a streaming call has to be a config

        clientStream.send(request);
    }

    public void send(String message) {
        try {
            JSONObject jo = new JSONObject(message);
            if (!jo.getString("event").equals("media")) {
                return;
            }

            String payload = jo.getJSONObject("media").getString("payload");
            byte[] data = Base64.getDecoder().decode(payload);
            StreamingRecognizeRequest request =
                StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(data))
                    .build();
            clientStream.send(request);

        } catch (JSONException e) {
            logger.error("Unrecognized JSON");
            e.printStackTrace();
        }

    }

    public void close() {
        logger.info("Closed");
        responseObserver.onComplete();
    }

}
