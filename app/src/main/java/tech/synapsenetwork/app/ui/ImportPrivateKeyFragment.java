package tech.synapsenetwork.app.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import tech.synapsenetwork.app.ui.widget.OnImportPrivateKeyListener;

public class ImportPrivateKeyFragment extends Fragment implements View.OnClickListener {

    private static final OnImportPrivateKeyListener dummyOnImportPrivateKeyListener = key -> { };

    private EditText privateKey;
    private OnImportPrivateKeyListener onImportPrivateKeyListener;

    public static ImportPrivateKeyFragment create() {
        return new ImportPrivateKeyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext())
                .inflate(tech.synapsenetwork.app.R.layout.fragment_import_private_key, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        privateKey = view.findViewById(tech.synapsenetwork.app.R.id.private_key);
        view.findViewById(tech.synapsenetwork.app.R.id.import_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        privateKey.setError(null);
        String value = privateKey.getText().toString();
        if (TextUtils.isEmpty(value) || value.length() != 64) {
            privateKey.setError(getString(tech.synapsenetwork.app.R.string.error_field_required));
        } else {
            onImportPrivateKeyListener.onPrivateKey(privateKey.getText().toString());
        }
    }

    public void setOnImportPrivateKeyListener(OnImportPrivateKeyListener onImportPrivateKeyListener) {
        this.onImportPrivateKeyListener = onImportPrivateKeyListener == null
                ? dummyOnImportPrivateKeyListener
                : onImportPrivateKeyListener;
    }
}
