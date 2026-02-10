package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Получение владельца аккаунта"

    request {
        method GET()
        urlPath(regex("/accounts/[a-zA-Z0-9]+/owner"))
        headers {
            header("Authorization", regex("Bearer .*"))
        }
    }

    response {
        status OK()
        body(
                accountId: regex("[a-f0-9\\-]+"),
                ownerUsername: "ivanov123"
        )
    }
}
