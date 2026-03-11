package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Операция пополнения/списания средств"

    request {
        method POST()
        url "/accounts/cash"
        headers {
            contentType(applicationJson())
            header("Authorization", regex("Bearer .*"))
        }
        body(
                action: "PUT",
                value: 100.00,
                accountId: "uuid",
                accountLogin: "ivanov123"
        )
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                success: true,
                message: "Операция выполнена: 100.00",
                data: null
        )
    }
}
