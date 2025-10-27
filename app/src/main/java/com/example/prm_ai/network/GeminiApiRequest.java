package com.example.prm_ai.network;

import java.util.Collections;
import java.util.List;

public class GeminiApiRequest {
    private final List<Content> contents;

    public GeminiApiRequest(String text) {
        Part part = new Part(text);
        Content content = new Content(Collections.singletonList(part));
        this.contents = Collections.singletonList(content);
    }

    public static class Content {
        private final List<Part> parts;

        public Content(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private final String text;

        public Part(String text) {
            this.text = text;
        }
    }
}
