package local.wallet.analyzing.account;

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

import local.wallet.analyzing.transaction.FragmentDescription;
import local.wallet.analyzing.R;
import local.wallet.analyzing.utils.LogUtils;
import local.wallet.analyzing.main.ActivityMain;
import local.wallet.analyzing.model.Account;
import local.wallet.analyzing.model.Account.IAccountCallback;
import local.wallet.analyzing.model.AccountType;
import local.wallet.analyzing.model.Currency;
import local.wallet.analyzing.sqlite.helper.DatabaseHelper;
import local.wallet.analyzing.account.FragmentAccountTypeSelect.ISelectAccountType;
import local.wallet.analyzing.transaction.FragmentDescription.IUpdateDescription;
import local.wallet.analyzing.account.FragmentCurrencySelect.ISelectCurrency;

/**
 * Created by huynh.thanh.huan on 1/6/2016.
 */
public class FragmentAccountUpdate extends Fragment implements View.OnClickListener, IUpdateDescription, ISelectAccountType, ISelectCurrency {
    public static int                   mTab = 2;
    public static final String          Tag = "---[" + mTab + "]---AccountUpdate";

    private ActivityMain                mActivity;

    private DatabaseHelper      mDbHelper;

    private Account             mAccount;
    private int                 mAccountId = 0;
    private IAccountCallback    mCallback;
    private int                 mContainerViewId;

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
        LogUtils.logEnterFunction(Tag);

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mDbHelper           = new DatabaseHelper(getActivity());

        Bundle bundle       = this.getArguments();
        mTab                = bundle.getInt("Tab", mTab);
        mAccountId          = bundle.getInt("AccountID", 0);
		mAccount            = mDbHelper.getAccount(mAccountId);
        mCallback           = (IAccountCallback) bundle.getSerializable("Callback");
        mContainerViewId    = bundle.getInt("ContainerViewId");

        LogUtils.logLeaveFunction(Tag);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);

        View    view        = inflater.inflate(R.layout.layout_fragment_account_update, container, false);

        // Initialize View
        etName              = (ClearableEditText)   view.findViewById(R.id.etName);
        llType              = (LinearLayout)        view.findViewById(R.id.llType);
        tvType              = (TextView)            view.findViewById(R.id.tvType);
        llCurrency          = (LinearLayout)        view.findViewById(R.id.llCurrency);
        tvCurrency          = (TextView)            view.findViewById(R.id.tvCurrency);
        etInitialBalance    = (EditText)            view.findViewById(R.id.etInitialBalance);
        tvCurrencyIcon      = (TextView)            view.findViewById(R.id.tvCurrencyIcon);
        llDescription       = (LinearLayout)        view.findViewById(R.id.llDescription);
        tvDescription       = (TextView)            view.findViewById(R.id.tvDescription);
        llSave              = (LinearLayout)        view.findViewById(R.id.llSave);
        llDelete            = (LinearLayout)        view.findViewById(R.id.llDelete);

        etName.setText(mAccount.getName());
        tvType.setText(AccountType.getAccountTypeById(mAccount.getTypeId()).getName());
        tvCurrency.setText(Currency.getCurrencyName(Currency.getCurrencyById(mAccount.getCurrencyId())));
        etInitialBalance.setText(Currency.formatCurrencyDouble(mAccount.getCurrencyId(), mAccount.getInitBalance()));
        tvCurrencyIcon.setText(Currency.getCurrencyIcon(mAccount.getCurrencyId()));
        tvDescription.setText(mAccount.getDescription());

        etInitialBalance.addTextChangedListener(new CurrencyTextWatcher());
        llType.setOnClickListener(this);
        llCurrency.setOnClickListener(this);
        llDescription.setOnClickListener(this);
        llSave.setOnClickListener(this);
        llDelete.setOnClickListener(this);

        LogUtils.logLeaveFunction(Tag);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag);
        super.onActivityCreated(savedInstanceState);

        mActivity   = (ActivityMain) getActivity();

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogUtils.logEnterFunction(Tag);
        super.onCreateOptionsMenu(menu, inflater);

        if(mTab != mActivity.getCurrentVisibleItem()) {
            LogUtils.error(Tag, "Wrong Tab. Return");
            LogUtils.logLeaveFunction(Tag);
            return;
        }

        LayoutInflater mInflater    = LayoutInflater.from(getActivity());
        View mCustomView            = mInflater.inflate(R.layout.action_bar_only_title, null);
        TextView tvTitle            = (TextView) mCustomView.findViewById(R.id.tvTitle);
        tvTitle.setText(getResources().getString(R.string.title_account_add));

        ((ActivityMain) getActivity()).updateActionBar(mCustomView);

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onAccountTypeSelected(int accountTypeId) {
        LogUtils.logEnterFunction(Tag, "accountTypeId = " + accountTypeId);

        mAccount.setTypeId(accountTypeId);
        tvType.setText(AccountType.getAccountTypeById(accountTypeId).getName());

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onCurrencySelected(Currency.CurrencyList currency) {
        LogUtils.logEnterFunction(Tag, "currency = " + currency);

        mAccount.setCurrencyId(currency.getValue());
        tvCurrency.setText(Currency.getCurrencyName(Currency.getCurrencyById(mAccount.getCurrencyId())));
        tvCurrencyIcon.setText(Currency.getCurrencyIcon(mAccount.getCurrencyId()));

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onDescriptionUpdated(String description) {
        LogUtils.logEnterFunction(Tag, "description = " + description);

        tvDescription.setText(description);

        LogUtils.logLeaveFunction(Tag);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llType: {
                FragmentAccountTypeSelect nextFrag = new FragmentAccountTypeSelect();
                Bundle bundle = new Bundle();
                bundle.putInt("Tab", mTab);
                bundle.putInt("AccountType", mAccount.getTypeId());
                bundle.putSerializable("Callback", FragmentAccountUpdate.this);
                nextFrag.setArguments(bundle);
                mActivity.addFragment(mTab, mContainerViewId, nextFrag, FragmentAccountTypeSelect.Tag, true);
                break;
            }
            case R.id.llCurrency: {
                FragmentCurrencySelect nextFrag = new FragmentCurrencySelect();
                Bundle bundle = new Bundle();
                bundle.putInt("Tab", mTab);
                bundle.putInt("Currency", mAccount.getCurrencyId());
                nextFrag.setArguments(bundle);
                mActivity.addFragment(mTab, mContainerViewId, nextFrag, FragmentCurrencySelect.Tag, true);
                break;
            }
            case R.id.llDescription: {
                FragmentDescription nextFrag = new FragmentDescription();
                Bundle bundle = new Bundle();
                bundle.putInt("Tab", mTab);
                bundle.putString("Description", tvDescription.getText().toString());
                bundle.putSerializable("Callback", FragmentAccountUpdate.this);
                nextFrag.setArguments(bundle);
                mActivity.addFragment(mTab, mContainerViewId, nextFrag, FragmentDescription.Tag, true);
                break;
            }
            case R.id.llSave: {
                LogUtils.trace(Tag, "Click button SAVE");
                // Check Account's name
                if (etName.getText().toString().equals("")) {
                    LogUtils.trace(Tag, "Name is empty");
                    etName.setError(getResources().getString(R.string.Input_Error_Account_Name_Empty));
                    return;
                }

                String accountName = etName.getText().toString();
                Double initialBalance = etInitialBalance.getText().toString().equals("") ? 0 : Double.parseDouble(etInitialBalance.getText().toString().replaceAll(",", ""));
                String description = tvDescription.getText().toString();

                // Update account in DB
                mDbHelper.updateAccount(new Account(mAccountId, accountName, mAccount.getTypeId(), mAccount.getCurrencyId(), initialBalance, description));

                // Update list of Account in FragmentListAccount
                mCallback.onListAccountUpdated();

                // Return to FragmentListAccount
                getFragmentManager().popBackStackImmediate();
                break;
            }
            case R.id.llDelete: {
                // Todo: Confirm, delete all transaction
                // Delete all related Transaction with this Account
                mDbHelper.deleteAllTransaction(mAccountId);

                // Delete this Account
                mDbHelper.deleteAccount(mAccountId);

                // Update list of Account in FragmentListAccount
                mCallback.onListAccountUpdated();

                // Return to FragmentListAccount
                getFragmentManager().popBackStackImmediate();
                break;
            }
            default:
                break;
        }
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
            LogUtils.logEnterFunction(Tag);

            if(!s.toString().equals(current)){
                etInitialBalance.removeTextChangedListener(this);

                LogUtils.trace(Tag, "input: " + s.toString());
                String inputted = s.toString().trim().replaceAll(",", "").replaceAll(" ", "");
                if(inputted.equals("")) {
                    return;
                }
                String formatted = Currency.formatCurrencyDouble(mAccount.getCurrencyId(), Double.parseDouble(inputted));

                current = formatted;
                etInitialBalance.setText(formatted);
                etInitialBalance.setSelection(formatted.length());

                etInitialBalance.addTextChangedListener(this);
            }

            LogUtils.logLeaveFunction(Tag);
        }

    }
}
