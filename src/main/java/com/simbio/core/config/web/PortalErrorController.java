package com.simbio.core.config.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PortalErrorController implements ErrorController {

    @RequestMapping("/error")
    public ModelAndView error(HttpServletRequest request) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttribute instanceof Integer code ? code : 500;
        String view = switch (status) {
            case 403 -> "errors/403";
            case 404 -> "errors/404";
            default -> "errors/500";
        };
        ModelAndView modelAndView = new ModelAndView(view);
        modelAndView.setStatus(HttpStatusCode.valueOf(status));
        return modelAndView;
    }
}
