package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Получение текущего аккаунта"

    request {
        method GET()
        url "/accounts/me"
        headers {
            header"Authorization", value(
                    consumer(regex('Bearer\\s+.+')),
                    producer('Bearer test-token'))
        }
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
