package tech.synapsenetwork.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import tech.synapsenetwork.app.Constants;
import tech.synapsenetwork.app.entity.ErrorEnvelope;
import tech.synapsenetwork.app.entity.GasSettings;
import tech.synapsenetwork.app.entity.Wallet;
import tech.synapsenetwork.app.util.BalanceUtils;
import tech.synapsenetwork.app.viewmodel.ConfirmationViewModel;
import tech.synapsenetwork.app.viewmodel.ConfirmationViewModelFactory;
import tech.synapsenetwork.app.viewmodel.GasSettingsViewModel;

import java.math.BigInteger;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class ConfirmationActivity extends BaseActivity {
    AlertDialog dialog;

    @Inject
    ConfirmationViewModelFactory confirmationViewModelFactory;
    ConfirmationViewModel viewModel;

    private TextView fromAddressText;
    private TextView toAddressText;
    private TextView valueText;
    private TextView gasPriceText;
    private TextView gasLimitText;
    private TextView networkFeeText;
    private Button sendButton;

    private BigInteger amount;
    private int decimals;
    private String contractAddress;
    private boolean confirmationForTokenTransfer = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(tech.synapsenetwork.app.R.layout.activity_confirm);
        toolbar();

        fromAddressText = findViewById(tech.synapsenetwork.app.R.id.text_from);
        toAddressText = findViewById(tech.synapsenetwork.app.R.id.text_to);
        valueText = findViewById(tech.synapsenetwork.app.R.id.text_value);
        gasPriceText = findViewById(tech.synapsenetwork.app.R.id.text_gas_price);
        gasLimitText = findViewById(tech.synapsenetwork.app.R.id.text_gas_limit);
        networkFeeText = findViewById(tech.synapsenetwork.app.R.id.text_network_fee);
        sendButton = findViewById(tech.synapsenetwork.app.R.id.send_button);

        sendButton.setOnClickListener(view -> onSend());

        String toAddress = getIntent().getStringExtra(Constants.EXTRA_TO_ADDRESS);
        contractAddress = getIntent().getStringExtra(Constants.EXTRA_CONTRACT_ADDRESS);
        amount = new BigInteger(getIntent().getStringExtra(Constants.EXTRA_AMOUNT));
        decimals = getIntent().getIntExtra(Constants.EXTRA_DECIMALS, -1);
        String symbol = getIntent().getStringExtra(Constants.EXTRA_SYMBOL);
        symbol = symbol == null ? Constants.ETH_SYMBOL : symbol;

        confirmationForTokenTransfer = contractAddress != null;

        toAddressText.setText(toAddress);

        String amountString = "-" + BalanceUtils.subunitToBase(amount, decimals).toPlainString() + " " + symbol;
        valueText.setText(amountString);
        valueText.setTextColor(ContextCompat.getColor(this, tech.synapsenetwork.app.R.color.red));

        viewModel = ViewModelProviders.of(this, confirmationViewModelFactory)
                .get(ConfirmationViewModel.class);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.gasSettings().observe(this, this::onGasSettings);
        viewModel.sendTransaction().observe(this, this::onTransaction);
        viewModel.progress().observe(this, this::onProgress);
        viewModel.error().observe(this, this::onError);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(tech.synapsenetwork.app.R.menu.confirmation_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case tech.synapsenetwork.app.R.id.action_edit: {
                viewModel.openGasSettings(ConfirmationActivity.this);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        viewModel.prepare(confirmationForTokenTransfer);
    }

    private void onProgress(boolean shouldShowProgress) {
        hideDialog();
        if (shouldShowProgress) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(tech.synapsenetwork.app.R.string.title_dialog_sending)
                    .setView(new ProgressBar(this))
                    .setCancelable(false)
                    .create();
            dialog.show();
        }
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void onSend() {
        GasSettings gasSettings = viewModel.gasSettings().getValue();

        if (!confirmationForTokenTransfer) {
            viewModel.createTransaction(
                    fromAddressText.getText().toString(),
                    toAddressText.getText().toString(),
                    amount,
                    gasSettings.gasPrice,
                    gasSettings.gasLimit);
        } else {
            viewModel.createTokenTransfer(
                    fromAddressText.getText().toString(),
                    toAddressText.getText().toString(),
                    contractAddress,
                    amount,
                    gasSettings.gasPrice,
                    gasSettings.gasLimit);
        }
    }

    private void onDefaultWallet(Wallet wallet) {
        fromAddressText.setText(wallet.address);
    }

    private void onTransaction(String hash) {
        hideDialog();
        dialog = new AlertDialog.Builder(this)
                .setTitle(tech.synapsenetwork.app.R.string.transaction_succeeded)
                .setMessage(hash)
                .setPositiveButton(tech.synapsenetwork.app.R.string.button_ok, (dialog1, id) -> {
                    finish();
                })
                .setNeutralButton(tech.synapsenetwork.app.R.string.copy, (dialog1, id) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("transaction hash", hash);
                    clipboard.setPrimaryClip(clip);
                    finish();
                })
                .create();
        dialog.show();
    }

    private void onGasSettings(GasSettings gasSettings) {
        String gasPrice = BalanceUtils.weiToGwei(gasSettings.gasPrice) + " " + Constants.GWEI_UNIT;
        gasPriceText.setText(gasPrice);
        gasLimitText.setText(gasSettings.gasLimit.toString());

        String networkFee = BalanceUtils.weiToEth(gasSettings
                .gasPrice.multiply(gasSettings.gasLimit)).toPlainString() + " " + Constants.ETH_SYMBOL;
        networkFeeText.setText(networkFee);
    }

    private void onError(ErrorEnvelope error) {
        hideDialog();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(tech.synapsenetwork.app.R.string.error_transaction_failed)
                .setMessage(error.message)
                .setPositiveButton(tech.synapsenetwork.app.R.string.button_ok, (dialog1, id) -> {
                    // Do nothing
                })
                .create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == GasSettingsViewModel.SET_GAS_SETTINGS) {
            if (resultCode == RESULT_OK) {
                BigInteger gasPrice = new BigInteger(intent.getStringExtra(Constants.EXTRA_GAS_PRICE));
                BigInteger gasLimit = new BigInteger(intent.getStringExtra(Constants.EXTRA_GAS_LIMIT));
                GasSettings settings = new GasSettings(gasPrice, gasLimit);
                viewModel.gasSettings().postValue(settings);
            }
        }
    }
}
