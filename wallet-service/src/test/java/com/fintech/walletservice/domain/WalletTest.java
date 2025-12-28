package com.fintech.walletservice.domain;

import com.fintech.walletservice.exception.InsufficientFundsException;
import com.fintech.walletservice.exception.WalletNotActiveException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class WalletTest {

    @Test
    void shouldCreditFunds() {
        Wallet wallet = Wallet.builder()
                .balanceMinorUnits(1000L)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        wallet.credit(500L);

        assertThat(wallet.getBalanceMinorUnits()).isEqualTo(1500L);
    }

    @Test
    void shouldDebitFunds() {
        Wallet wallet = Wallet.builder()
                .balanceMinorUnits(1000L)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        wallet.debit(300L);

        assertThat(wallet.getBalanceMinorUnits()).isEqualTo(700L);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientFunds() {
        Wallet wallet = Wallet.builder()
                .balanceMinorUnits(100L)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> wallet.debit(200L))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void shouldThrowExceptionWhenWalletFrozen() {
        Wallet wallet = Wallet.builder()
                .balanceMinorUnits(1000L)
                .status(Wallet.WalletStatus.FROZEN)
                .build();

        assertThatThrownBy(() -> wallet.debit(100L))
                .isInstanceOf(WalletNotActiveException.class);
    }

    @Test
    void shouldRejectNegativeAmounts() {
        Wallet wallet = Wallet.builder()
                .balanceMinorUnits(1000L)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> wallet.credit(-100L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> wallet.debit(-100L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
