package com.example.utils;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import org.primefaces.PrimeFaces;
import java.io.Serializable;

public class MessageHandler implements Serializable {
    private static final long serialVersionUID = 1L;

    public void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, detail));
    }

    public void addInfoMessage(String summary, String detail, String... clientIds) {
        addMessage(FacesMessage.SEVERITY_INFO, summary, detail);
        updateClientIds(clientIds);
    }

    public void addErrorMessage(String summary, String detail, String... clientIds) {
        addMessage(FacesMessage.SEVERITY_ERROR, summary, detail);
        updateClientIds(clientIds);
    }

    public void addWarnMessage(String summary, String detail, String... clientIds) {
        addMessage(FacesMessage.SEVERITY_WARN, summary, detail);
        updateClientIds(clientIds);
    }

    public void addFatalMessage(String summary, String detail, String... clientIds) {
        addMessage(FacesMessage.SEVERITY_FATAL, summary, detail);
        updateClientIds(clientIds);
    }

    private void updateClientIds(String... clientIds) {
        if (clientIds != null && clientIds.length > 0) {
            PrimeFaces.current().ajax().update(clientIds);
        }
    }

    public void addExceptionMessage(String summary, Exception exception, String... clientIds) {
        String detail = exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred";
        addErrorMessage(summary, detail, clientIds);
    }
}