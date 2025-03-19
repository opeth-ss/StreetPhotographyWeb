package com.example.security;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class AuthenticationPhaseListener implements PhaseListener {
    private static final long serialVersionUID = 1L;

    @Override
    public void afterPhase(PhaseEvent event) {
        FacesContext facesContext = event.getFacesContext();
        String currentPage = facesContext.getViewRoot().getViewId();

        boolean isLoginPage = currentPage.lastIndexOf("login.xhtml") > -1;
        boolean isRegisterPage = currentPage.lastIndexOf("register.xhtml") > -1;
        boolean isPublicPage = isLoginPage || isRegisterPage;

        if (!isPublicPage) {
            Object currentUser = facesContext.getExternalContext().getSessionMap().get("loggedInUser");

            if (currentUser == null) {
                NavigationHandler nh = facesContext.getApplication().getNavigationHandler();
                nh.handleNavigation(facesContext, null, "/pages/login.xhtml?faces-redirect=true");
            }
        }
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        // Not needed for login check
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }
}
