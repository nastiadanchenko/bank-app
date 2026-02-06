package yandex.workshop.frontui.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountDto(
    String login,
    String name,
    LocalDate birthdate,
    BigDecimal balance
) {}

