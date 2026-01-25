package yandex.workshop.accountsservice;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;


@Data
@Entity
@Table(schema = "accounts")
public class Account {


    @Id
    private Long id;

    @Column(unique = true)
    private String login;

    private String firstName;
    private String lastName;

    private LocalDate birthDate;

    private BigDecimal balance;


}

