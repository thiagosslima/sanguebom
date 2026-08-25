package br.com.fiap.sanguebom.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HomeResource {

    @GetMapping("/")
    public String index() {
        return "\"Hello World!\"";
    }

}
