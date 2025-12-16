package com.didiglobal.turbo.engine.exception;

public  class ForkJoinValidationException extends Exception {
        private final String elementKey;
        private final String elementName;
        private final String type;

        public ForkJoinValidationException(String elementKey, String elementName, String type, String message) {
            super(message);
            this.elementKey = elementKey;
            this.elementName = elementName;
            this.type = type;
        }

        public String getElementKey() { return elementKey; }
        public String getElementName() { return elementName; }
        public String getType() { return type; }
    }