package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Обновление профиля аккаунта"

    request {
        method POST()
        url "/accounts/me"
        headers {
            contentType(applicationJson())
            header("Authorization", regex("Bearer .*"))
        }
        body(
                name: "Иванов Иван",
                birthdate: "1990-01-01"
        )
    }

    response {
        status OK()
        body(
                login: "ivanov123",
                name: "Иванов Иван",
                birthdate: "1990-01-01",
                balance: 1000.00
        )
    }
}
