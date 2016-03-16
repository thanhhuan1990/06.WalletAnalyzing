package local.wallet.analyzing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.droidparts.widget.ClearableEditText;

import local.wallet.analyzing.Utils.LogUtils;
import local.wallet.analyzing.model.Account;
import local.wallet.analyzing.model.AccountType;
import local.wallet.analyzing.model.Currency;
import local.wallet.analyzing.sqlite.helper.DatabaseHelper;

/**
 * Created by huynh.thanh.huan on 1/6/2016.
 */
public class FragmentAccountUpdate extends Fragment {

    private static final String Tag = "AccountUpdate";

    private DatabaseHelper      mDbHelper;

    private Account             mAccount;
    private int                 mAccountId = 0;

    private ClearableEditText   etName;
    private LinearLayout        llType;
    private TextView            tvType;
    private LinearLayout        llCurrency;
    private TextView            tvCurrency;
    private EditText            etInitialBalance;
    private TextView            tvCurrencyIcon;
    private LinearLayout        llDescription;
    private TextView            tvDescription;
    private LinearLayout        llSave;
    private LinearLayout        llDelete;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag, null);

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle bundle = this.getArguments();
        mAccountId = bundle.getInt("AccountID", 0);

        LogUtils.logLeaveFunction(Tag, null, null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogUtils.logEnterFunction(Tag, null);

        LayoutInflater mInflater = LayoutInflater.from(getActivity());
        View mCustomView = mInflater.inflate(R.layout.action_bar_only_title, null);
        TextView tvTitle = (TextView) mCustomView.findViewById(R.id.tvTitle);
        tvTitle.setText(getResources().getString(R.string.title_account_add));
        ((ActivityMain) getActivity()).updateActionBar(mCustomView);

        super.onCreateOptionsMenu(menu, inflater);

        LogUtils.logLeaveFunction(Tag, null, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag, null);

        String myTag = getTag();
        ((ActivityMain)getActivity()).setFragmentAccountUpdate(myTag);

        LogUtils.logLeaveFunction(Tag, null, null);

        return inflater.inflate(R.layout.layout_fragment_account_update, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag, null);

        super.onActivityCreated(savedInstanceState);

        mDbHelper           = new DatabaseHelper(getActivity());
        mAccount            = mDbHelper.getAccount(mAccountId);

        // Initialize View
        etName = (ClearableEditText) getView().findViewById(R.id.etName);
        llType              = (LinearLayout) getView().findViewById(R.id.llType);
        tvType              = (TextView) getView().findViewById(R.id.tvType);
        llCurrency          = (LinearLayout) getView().findViewById(R.id.llCurrency);
        tvCurrency          = (TextView) getView().findViewById(R.id.tvCurrency);
        etInitialBalance    = (EditText) getView().findViewById(R.id.etInitialBalance);
        tvCurrencyIcon      = (TextView) getView().findViewById(R.id.tvCurrencyIcon);
        llDescription       = (LinearLayout) getView().findViewById(R.id.llDescription);
        tvDescription       = (TextView) getView().findViewById(R.id.tvDescription);
        llSave              = (LinearLayout) getView().findViewById(R.id.llSave);
        llDelete            = (LinearLayout) getView().findViewById(R.id.llDelete);

        etName.setText(mAccount.getName());
        tvType.setText(AccountType.getAccountTypeById(mAccount.getTypeId()).getName());
        tvCurrency.setText(Currency.getCurrencyName(Currency.getCurrencyById(mAccount.getCurrencyId())));
        etInitialBalance.setText(Currency.formatCurrencyDouble(Currency.getCurrencyById(mAccount.getCurrencyId()), mAccount.getInitBalance()));
        tvCurrencyIcon.setText(Currency.getCurrencyIcon(Currency.getCurrencyById(mAccount.getCurrencyId())));
        tvDescription.setText(mAccount.getDescription());

        llType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAccountTypeSelect nextFrag = new FragmentAccountTypeSelect();
                Bundle bundle = new Bundle();
                bundle.putString("Tag", ((ActivityMain)getActivity()).getFragmentAccountUpdate());
                bundle.putInt("AccountType", mAccount.getTypeId());
                nextFrag.setArguments(bundle);
                FragmentAccountUpdate.this.getFragmentManager().beginTransaction()
                        .add(R.id.layout_account, nextFrag, "FragmentAccountTypeSelect")
                        .addToBackStack(null)
                        .commit();
            }
        });

        llCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentCurrencySelect nextFrag = new FragmentCurrencySelect();
                Bundle bundle = new Bundle();
                bundle.putString("Tag", ((ActivityMain)getActivity()).getFragmentAccountUpdate());
                bundle.putInt("Currency", mAccount.getCurrencyId());
                nextFrag.setArguments(bundle);
                FragmentAccountUpdate.this.getFragmentManager().beginTransaction()
                        .add(R.id.layout_account, nextFrag, "FragmentCurrencySelect")
                        .addToBackStack(null)
                        .commit();
            }
        });

        etInitialBalance.addTextChangedListener(new CurrencyTextWatcher());

        llDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentDescription nextFrag = new FragmentDescription();
                Bundle bundle = new Bundle();
                bundle.putString("Tag", ((ActivityMain)getActivity()).getFragmentAccountUpdate());
                bundle.putString("Description", tvDescription.getText().toString());
                nextFrag.setArguments(bundle);
                FragmentAccountUpdate.this.getFragmentManager().beginTransaction()
                        .add(R.id.layout_account, nextFrag, "FragmentDescription")
                        .addToBackStack(null)
                        .commit();
            }
        });

        llSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.trace(Tag, "Click button SAVE");
                // Check Account's name
                if(etName.getText().toString().equals("")) {
                    LogUtils.trace(Tag, "Name is empty");
                    etName.setError(getResources().getString(R.string.Input_Error_Account_Name_Empty));
                    return;
                }

                String accountName = etName.getText().toString();
                Double initialBalance =  etInitialBalance.getText().toString().equals("") ? 0 : Double.parseDouble(etInitialBalance.getText().toString().replaceAll(",", ""));
                String description = tvDescription.getText().toString();


                // Update account in DB
                mDbHelper.updateAccount(new Account(mAccountId, accountName, mAccount.getTypeId(), mAccount.getCurrencyId(), initialBalance, description));

                // Update list of Account in FragmentListAccount
                FragmentListAccount fragmentListAccount = (FragmentListAccount)((ActivityMain)getActivity()).getFragment(ActivityMain.TAB_POSITION_LIST_ACCOUNT);
                fragmentListAccount.updateToAccountList(mDbHelper.getAccount(mAccountId));

                // Return to FragmentListAccount
                getFragmentManager().popBackStackImmediate();
            }
        });

        llDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDbHelper.deleteAccount(mAccountId);

                // Update list of Account in FragmentListAccount
                FragmentListAccount fragmentListAccount = (FragmentListAccount)((ActivityMain)getActivity()).getFragment(ActivityMain.TAB_POSITION_LIST_ACCOUNT);
                fragmentListAccount.removeToAccountList(mAccountId);

                // Return to FragmentListAccount
                getFragmentManager().popBackStackImmediate();
            }
        });


        LogUtils.logLeaveFunction(Tag, null, null);
    }

    /**
     * Update AccountType, call from ActivityMain
     * @param accountTypeId
     */
    public void updateAccountType(int accountTypeId) {
        LogUtils.logEnterFunction(Tag, "accountTypeId = " + accountTypeId);

        mAccount.setTypeId(accountTypeId);
        tvType.setText(AccountType.getAccountTypeById(accountTypeId).getName());

        LogUtils.logLeaveFunction(Tag, "accountTypeId = " + accountTypeId, null);
    }

    /**
     * Update Currency, call from ActivityMain
     * @param currency
     */
    public void updateCurrency(Currency.CurrencyList currency) {
        LogUtils.logEnterFunction(Tag, "currency = " + currency);

        mAccount.setCurrencyId(currency.getValue());
        tvCurrency.setText(Currency.getCurrencyName(Currency.getCurrencyById(mAccount.getCurrencyId())));
        tvCurrencyIcon.setText(Currency.getCurrencyIcon(Currency.getCurrencyById(mAccount.getCurrencyId())));

        LogUtils.logLeaveFunction(Tag, "currency = " + currency, null);
    }

    /**
     * Update Description, call from ActivityMain
     * @param description
     */
    public void updateDescription(String description) {
        LogUtils.logEnterFunction(Tag, "description = " + description);

        tvDescription.setText(description);

        LogUtils.logLeaveFunction(Tag, "description = " + description, null);
    }

    /**
     * Initial Balance EditText's TextWatcher
     */
    private class CurrencyTextWatcher implements TextWatcher {
        private String current = "";

        public CurrencyTextWatcher() {}

        public synchronized void afterTextChanged(Editable s) {}

        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            LogUtils.logEnterFunction(Tag, null);

            if(!s.toString().equals(current)){
                etInitialBalance.removeTextChangedListener(this);

                LogUtils.trace(Tag, "input: " + s.toString());
                String inputted = s.toString().trim().replaceAll(",", "").replaceAll(" ", "");
                if(inputted.equals("")) {
                    return;
                }
                String formatted = Currency.formatCurrencyDouble(Currency.getCurrencyById(mAccount.getCurrencyId()), Double.parseDouble(inputted));

                current = formatted;
                etInitialBalance.setText(formatted);
                etInitialBalance.setSelection(formatted.length());

                etInitialBalance.addTextChangedListener(this);
            }

            LogUtils.logLeaveFunction(Tag, null, null);
        }

    }
}
