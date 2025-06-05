package com.jpmc.midascore.controller;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.foundation.Balance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceController {

    private final DatabaseConduit databaseConduit;

    @Autowired
    public BalanceController(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }

    @GetMapping("/balance")
    public Balance getBalance(@RequestParam("userId") long userId) {
        float balance = databaseConduit.getUserBalanceById(userId);
        return new Balance(balance);
    }
}
