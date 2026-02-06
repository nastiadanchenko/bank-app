package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Перевод средств между счетами"

    request {
        method POST()
        url "/accounts/transfer"
        headers {
            contentType(applicationJson())
            header("Authorization", regex("Bearer .*"))
        }
        body(
                fromLogin: "ivanov123",
                toLogin: "petrov456",
                amount: 100.00
        )
    }

    response {
        status OK()
        body("Перевод выполнен: 100.00 со счёта ivanov123 на счёт petrov456")
    }
}
