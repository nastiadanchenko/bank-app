package yandex.workshop.frontui.exceptionHendler;

import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class FrontExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handle(Exception ex, RedirectAttributes attrs) {
        attrs.addFlashAttribute("errors", List.of(ex.getMessage()));
        return "redirect:/";
    }
}

