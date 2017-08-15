package com.equaleyes.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * Created by zan on 8/15/17.
 */

public class ProcessingException extends Exception {
    private final Element element;
    private final String msg;
    private final Object[] params;

    public ProcessingException(Element element, String msg, Object... params) {
        this.element = element;
        this.msg = msg;
        this.params = params;
    }

    void print(Messager messager) {
        String message = msg;
        if (params.length > 0) {
            message = String.format(msg, params);
        }
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
