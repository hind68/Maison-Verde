package com.portfolio.hotel.security;

import com.portfolio.hotel.user.service.UserAdministrationService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserAdministrationService service;
    private final List<String> values;

    public AdminBootstrap(
            UserAdministrationService service,
            @Value("${app.bootstrap-admin.login:}") String login,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.first-name:}") String firstName,
            @Value("${app.bootstrap-admin.last-name:}") String lastName
    ) {
        this.service = service;
        this.values = List.of(login, email, password, firstName, lastName);
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean any = values.stream().anyMatch(value -> !value.isBlank());
        if (!any) return;
        if (values.stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException(
                    "Tous les paramètres APP_BOOTSTRAP_ADMIN_* doivent être fournis ensemble.");
        }
        service.createBootstrapAdmin(values.get(0), values.get(1), values.get(2), values.get(3), values.get(4));
    }
}
