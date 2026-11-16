package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.port.in.WalletUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.controllers.mapper.UiModelMapper;
import ar.edu.uade.skillswap.model.Models;
import java.util.List;
import java.util.Objects;

/**
 * Pantalla SkillCoins: valida que el usuario sea el activo y delega en el caso de uso de su
 * billetera. El saldo no negativo y el registro de cada movimiento son reglas de {@code Wallet}.
 */
public final class WalletUiController implements UiControllers.WalletController {
    private final WalletUseCases wallets;
    private final CurrentUserProvider identity;
    private final ControllerExecutor operations;
    private final UiModelMapper mapper = new UiModelMapper();

    public WalletUiController(WalletUseCases wallets, CurrentUserProvider identity, ControllerExecutor operations) {
        this.wallets = Objects.requireNonNull(wallets);
        this.identity = Objects.requireNonNull(identity);
        this.operations = Objects.requireNonNull(operations);
    }

    @Override
    public int consultarSaldo(String userId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return wallets.viewMyWallet().balanceInSkillCoins();
        });
    }

    @Override
    public List<Models.Movimiento> listarMovimientos(String userId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return wallets.viewMyWallet().movements().stream().map(mapper::toMovimiento).toList();
        });
    }

    /** Créditos de demostración: el importe lo valida la billetera (tiene que ser positivo). */
    @Override
    public int acreditarDemo(String userId, int importe) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return wallets.creditDemo(importe).balanceInSkillCoins();
        });
    }
}
