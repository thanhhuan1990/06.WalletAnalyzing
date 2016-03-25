package local.wallet.analyzing;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import org.droidparts.widget.ClearableEditText;
import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import local.wallet.analyzing.FragmentAccountsSelect.ISelectAccount;
import local.wallet.analyzing.FragmentDescription.IUpdateDescription;
import local.wallet.analyzing.FragmentEvent.IUpdateEvent;
import local.wallet.analyzing.FragmentPayee.IUpdatePayee;
import local.wallet.analyzing.FragmentTransactionSelectCategory.ISelectCategory;
import local.wallet.analyzing.Utils.LogUtils;
import local.wallet.analyzing.model.Account;
import local.wallet.analyzing.model.Category;
import local.wallet.analyzing.model.Currency;
import local.wallet.analyzing.model.Event;
import local.wallet.analyzing.model.Transaction;
import local.wallet.analyzing.model.Transaction.TransactionEnum;
import local.wallet.analyzing.sqlite.helper.DatabaseHelper;

/**
 * Created by huynh.thanh.huan on 12/30/2015.
 */
public class FragmentTransactionCUDAdjustment extends Fragment implements  View.OnClickListener, ISelectCategory, ISelectAccount, IUpdateDescription, IUpdatePayee, IUpdateEvent {
    public static final String Tag = "TransactionCUDAdjustment";

    private Configurations      mConfigs;
    private DatabaseHelper      mDbHelper;

    private Category            mCategory;
    private Account             mFromAccount;

    private LinearLayout        llAccount;
    private TextView            tvAccount;
    private ClearableEditText   etBalance;
    private TextView            tvCurrencyIcon;
    private TextView            tvSpent;
    private LinearLayout        llCategory;
    private TextView            tvCategory;
    private LinearLayout        llPeople;
    private TextView            tvTitlePeople;
    private TextView            tvPeople;
    private LinearLayout        llDescription;
    private TextView            tvDescription;
    private LinearLayout        llDate;
    private TextView            tvDate;
    private LinearLayout        llPayee;
    private TextView            tvPayee;
    private LinearLayout        llEvent;
    private TextView            tvEvent;

    private LinearLayout        llSave;
    private LinearLayout        llDelete;

    private Calendar            mCal;

    private Transaction         mTransaction;

    private boolean             isExpense = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag, null);
        LogUtils.logLeaveFunction(Tag, null, null);
        return inflater.inflate(R.layout.layout_fragment_transaction_cud_adjustment, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag, null);
        super.onCreate(savedInstanceState);

        Bundle bundle           = this.getArguments();
        if(bundle != null) {
            mTransaction            = (Transaction)bundle.get("Transaction");

            LogUtils.trace(Tag, "mTransaction = " + mTransaction.toString());
        }

        LogUtils.logLeaveFunction(Tag, null, null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogUtils.logEnterFunction(Tag, null);

        super.onActivityCreated(savedInstanceState);

        mConfigs    = new Configurations(getActivity());
        mDbHelper   = new DatabaseHelper(getActivity());

        llSave      = (LinearLayout) getView().findViewById(R.id.llSave);
        llSave.setOnClickListener(this);
        llDelete    = (LinearLayout) getView().findViewById(R.id.llDelete);
        llDelete.setOnClickListener(this);

        if(mTransaction.getId() == 0) {
            llDelete.setVisibility(View.GONE);
        }

        mCategory       = mDbHelper.getCategory(mTransaction.getCategoryId());
        mFromAccount    = mTransaction.getFromAccountId() != 0 ? mDbHelper.getAccount(mTransaction.getFromAccountId()) : mDbHelper.getAllAccounts().get(0);
        mCal            = mTransaction.getTime();

        initView();

        LogUtils.logLeaveFunction(Tag, null, null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llAccount:
                ((ActivityMain) getActivity()).hideKeyboard(getActivity());
                startFragmentSelectAccount(TransactionEnum.Adjustment, mFromAccount != null ? mFromAccount.getId() : 0);
                break;
            case R.id.llCategory:
                ((ActivityMain) getActivity()).hideKeyboard(getActivity());
                startFragmentSelectCategory(mCategory != null ? mCategory.getId() : 0);
                break;
            case R.id.llDescription:
                ((ActivityMain) getActivity()).hideKeyboard(getActivity());
                startFragmentDescription(tvDescription.getText().toString());
                break;
            case R.id.llDate:
                ((ActivityMain) getActivity()).hideKeyboard(getActivity());
                showDialogTime();
                break;
            case R.id.llPayee:
                ((ActivityMain) getActivity()).hideKeyboard(getActivity());
                startFragmentPayee(tvPayee.getText().toString());
                break;
            case R.id.llEvent:
                ((ActivityMain) getActivity()).hideKeyboard(getActivity());
                startFragmentEvent(tvEvent.getText().toString());
                break;
            case R.id.llSave:
                ((ActivityMain) getActivity()).hideKeyboard(getActivity());
                if(mTransaction.getId() != 0) {
                    updateTransaction();
                } else {
                    createTransaction();
                }
                break;
            case R.id.llDelete:
                ((ActivityMain) getActivity()).hideKeyboard(getActivity());
                mDbHelper.deleteTransaction(mTransaction.getId());

                cleanup();

                // Return to FragmentListTransaction
                getFragmentManager().popBackStackImmediate();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCategorySelected(int categoryId) {
        LogUtils.logEnterFunction(Tag, "categoryId = " + categoryId);

        mCategory = mDbHelper.getCategory(categoryId);

        if(mCategory != null) {

            tvCategory.setText(mCategory.getName());

            switch (mCategory.getDebtType()) {
                case MORE:
                    llPeople.setVisibility(View.VISIBLE);
                    llPayee.setVisibility(View.GONE);
                    if(mCategory.isExpense()) {
                        tvTitlePeople.setText(getResources().getString(R.string.new_transaction_borrower));
                    } else {
                        tvTitlePeople.setText(getResources().getString(R.string.new_transaction_lender));
                    }
                    break;
                case NONE:
                    llPeople.setVisibility(View.GONE);
                    if(mCategory.isExpense()) {
                        llPayee.setVisibility(View.VISIBLE);
                    } else {
                        llPayee.setVisibility(View.GONE);
                    }
                    break;
                case LESS:
                    llPeople.setVisibility(View.VISIBLE);
                    llPayee.setVisibility(View.GONE);
                    if(mCategory.isExpense()) {
                        tvTitlePeople.setText(getResources().getString(R.string.new_transaction_lender));
                    } else {
                        tvTitlePeople.setText(getResources().getString(R.string.new_transaction_borrower));
                    }
                    break;
                default:
                    break;
            }
        } else {
            tvCategory.setText("");
            llPeople.setVisibility(View.GONE);

            if(isExpense) {
                llPayee.setVisibility(View.VISIBLE);
            } else {
                llPayee.setVisibility(View.GONE);
            }
        }

        LogUtils.logLeaveFunction(Tag, "categoryId = " + categoryId, null);
    }

    @Override
    public void onDescriptionUpdated(String description) {
        LogUtils.logEnterFunction(Tag, "description = " + description);

        tvDescription.setText(description);

        LogUtils.logLeaveFunction(Tag, "description = " + description, null);
    }

    @Override
    public void onAccountSelected(TransactionEnum type, int accountId) {
        LogUtils.logEnterFunction(Tag, "TransactionType = " + type.name() + ", accountId = " + accountId);

        if(type == TransactionEnum.Adjustment) {
            mFromAccount = mDbHelper.getAccount(accountId);
            tvAccount.setText(mFromAccount.getName());
            tvCurrencyIcon.setText(getResources().getString(Currency.getCurrencyIcon(mFromAccount.getCurrencyId())));
        }

        LogUtils.logLeaveFunction(Tag, "TransactionType = " + type.name() + ", accountId = " + accountId, null);
    }

    @Override
    public void onPayeeUpdated(String payee) {
        LogUtils.logEnterFunction(Tag, "payee = " + payee);

        tvPayee.setText(payee);

        LogUtils.logLeaveFunction(Tag, "payee = " + payee, null);
    }

    @Override
    public void onEventUpdated(String event) {
        LogUtils.logEnterFunction(Tag, "event = " + event);

        tvEvent.setText(event);

        LogUtils.logLeaveFunction(Tag, "event = " + event, null);
    }

    /**
     * Todo: Init view
     */
    private void initView() {
        LogUtils.logEnterFunction(Tag, null);

        llAccount       = (LinearLayout) getView().findViewById(R.id.llAccount);
        llAccount.setOnClickListener(this);
        tvAccount       = (TextView) getView().findViewById(R.id.tvAccount);

        etBalance       = (ClearableEditText) getView().findViewById(R.id.etBalance);
        etBalance.addTextChangedListener(new CurrencyTextWatcher(etBalance));

        tvCurrencyIcon  = (TextView) getView().findViewById(R.id.tvCurrencyIcon);

        tvSpent         = (TextView) getView().findViewById(R.id.tvSpent);

        llCategory      = (LinearLayout) getView().findViewById(R.id.llCategory);
        llCategory.setOnClickListener(this);
        tvCategory      = (TextView) getView().findViewById(R.id.tvCategory);

        llPeople        = (LinearLayout) getView().findViewById(R.id.llPeople);
        llPeople.setOnClickListener(this);
        tvTitlePeople   = (TextView) getView().findViewById(R.id.tvTitlePeople);
        tvPeople        = (TextView) getView().findViewById(R.id.tvPeople);

        llDescription   = (LinearLayout) getView().findViewById(R.id.llDescription);
        llDescription.setOnClickListener(this);
        tvDescription   = (TextView) getView().findViewById(R.id.tvDescription);

        llDate          = (LinearLayout) getView().findViewById(R.id.llDate);
        llDate.setOnClickListener(this);
        tvDate          = (TextView) getView().findViewById(R.id.tvDate);

        llPayee         = (LinearLayout) getView().findViewById(R.id.llPayee);
        llPayee.setOnClickListener(this);
        tvPayee         = (TextView) getView().findViewById(R.id.tvPayee);

        llEvent         = (LinearLayout) getView().findViewById(R.id.llEvent);
        llEvent.setOnClickListener(this);
        tvEvent         = (TextView) getView().findViewById(R.id.tvEvent);

        tvAccount.setText(mFromAccount != null ? mFromAccount.getName() : "");
        tvCategory.setText(mCategory != null ? mCategory.getName() : "");
        tvDescription.setText(mTransaction.getDescription());
        tvDate.setText(getDateString(mCal));
        tvPayee.setText(mTransaction.getPayee());
        tvEvent.setText(mTransaction.getEvent() != null ? mTransaction.getEvent().getName() : "");

        LogUtils.logLeaveFunction(Tag, null, null);
    }

    /**
     * Initial Balance EditText's TextWatcher
     */
    private class CurrencyTextWatcher implements TextWatcher {
        private String current = "";

        private EditText mEdittext;

        public CurrencyTextWatcher(EditText et) {
            mEdittext = et;
        }

        public synchronized void afterTextChanged(Editable s) {}

        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            LogUtils.logEnterFunction(Tag, null);

            if(!s.toString().equals(current)) {
                mEdittext.removeTextChangedListener(this);

                String inputted = s.toString().replaceAll(",", "").replaceAll(" ", "");

                String formatted = "";
                if (!inputted.equals("")) {
                    formatted = Currency.formatCurrencyDouble(mFromAccount != null ? mFromAccount.getCurrencyId() : mConfigs.getInt(Configurations.Key.Currency), Double.parseDouble(inputted));
                } else {
                    formatted = "0";
                }

                current = formatted;
                mEdittext.setText(formatted);
                mEdittext.setSelection(formatted.length());

                Double balance = !mEdittext.getText().toString().equals("") ? Double.parseDouble(mEdittext.getText().toString().replaceAll(",", "")) : 0;
                Double remain = mDbHelper.getAccountRemainBefore(mFromAccount.getId(), mCal);

                int compare = Double.compare(remain, balance);
                if(compare > 0) {
                    isExpense = true;
                    tvSpent.setText(String.format(getResources().getString(R.string.content_expensed),
                            Currency.formatCurrency(getContext(), mFromAccount.getCurrencyId(), remain - balance)));
                    ((TextView) getView().findViewById(R.id.tvTitleCategory)).setText(getResources().getString(R.string.transaction_category_expense));
                    if(mCategory != null) {
                        if(!mCategory.isExpense()) { // Current category is INCOME
                            if(mCategory.getDebtType() == Category.EnumDebt.LESS || mCategory.getDebtType() == Category.EnumDebt.MORE) {
                                List<Category> arTemp = mDbHelper.getAllParentCategories(true, mCategory.getDebtType());

                                if(arTemp.size() > 0) {
                                    onCategorySelected(arTemp.get(0).getId());
                                } else {
                                    onCategorySelected(0);
                                }
                            } else {
                                onCategorySelected(0);
                            }
                        }
                    } else {
                        onCategorySelected(0);
                    }
                } else if (compare < 0) {
                    isExpense = false;
                    tvSpent.setText(String.format(getResources().getString(R.string.content_adjustment_income),
                            Currency.formatCurrency(getContext(), mFromAccount.getCurrencyId(), balance - remain)));
                    ((TextView) getView().findViewById(R.id.tvTitleCategory)).setText(getResources().getString(R.string.transaction_category_income));
                    if(mCategory != null) {
                        if(mCategory.isExpense()) { // Current category is EXPENSE
                            if(mCategory.getDebtType() == Category.EnumDebt.LESS || mCategory.getDebtType() == Category.EnumDebt.MORE) {
                                List<Category> arTemp = mDbHelper.getAllParentCategories(false, mCategory.getDebtType());

                                if(arTemp.size() > 0) {
                                    onCategorySelected(arTemp.get(0).getId());
                                } else {
                                    onCategorySelected(0);
                                }
                            } else {
                                onCategorySelected(0);
                            }
                        }
                    } else {
                        onCategorySelected(0);
                    }
                } else {
                    isExpense = true;
                    tvSpent.setText(String.format(getResources().getString(R.string.content_expensed),
                            Currency.formatCurrency(getContext(), mFromAccount.getCurrencyId(), remain - balance)));
                    ((TextView) getView().findViewById(R.id.tvTitleCategory)).setText(getResources().getString(R.string.transaction_category_expense));
                    onCategorySelected(0);
                }

                mEdittext.addTextChangedListener(this);
            }

            LogUtils.logLeaveFunction(Tag, null, null);
        }

    } // End CurrencyTextWatcher

    /**
     * Save Transaction to Database
     */
    private void createTransaction() {
        LogUtils.logEnterFunction(Tag, null);
        if (mFromAccount == null) {
            ((ActivityMain) getActivity()).showError(getResources().getString(R.string.Input_Error_Account_Empty));
            return;
        }

        Double balance          = !etBalance.getText().toString().equals("") ? Double.parseDouble(etBalance.getText().toString().replaceAll(",", "")): 0;
        Double remain           = mDbHelper.getAccountRemainBefore(mFromAccount.getId(), mCal);

        if(remain.doubleValue() == balance.doubleValue()) {
            ((ActivityMain) getActivity()).showError(getResources().getString(R.string.Input_Error_Adjustment_Same_Amount));
            return;
        }

        int     categoryId      = mCategory != null ? mCategory.getId() : 0;
        String  description     = tvDescription.getText().toString();
        String  payee           = tvPayee.getText().toString();
        String  strEvent        = tvEvent.getText().toString();
        Event   event           = null;

        if(!strEvent.equals("")) {
            event = mDbHelper.getEventByName(strEvent);
            if(event == null) {
                long eventId = mDbHelper.createEvent(new Event(0, strEvent, mCal, null));
                if(eventId != -1) {
                    event = mDbHelper.getEvent(eventId);
                }
            }
        }

        Transaction transaction         = new Transaction(0,
                                                        TransactionEnum.Adjustment.getValue(),
                                                        Math.abs(remain - balance),
                                                        categoryId,
                                                        description,
                                                        remain > balance ? mFromAccount.getId() : 0,
                                                        remain > balance ? 0 : mFromAccount.getId(),
                                                        mCal,
                                                        0.0,
                                                        payee,
                                                        event);

        LogUtils.trace(Tag, "Transaction = " + transaction.toString());

        long newTransactionId = mDbHelper.createTransaction(transaction);

        if (newTransactionId != -1) {
            ((ActivityMain) getActivity()).showToastSuccessful(getResources().getString(R.string.message_transaction_create_successful));
            cleanup();
        }

        LogUtils.trace(Tag, "New Transaction ID = " + newTransactionId);

        LogUtils.logLeaveFunction(Tag, null, null);
    } // End createTransaction

    /**
     * Update transaction
     */
    private void updateTransaction() {
        LogUtils.logEnterFunction(Tag, null);

        if (mFromAccount == null) {
            ((ActivityMain) getActivity()).showError(getResources().getString(R.string.Input_Error_Account_Empty));
            return;
        }

        Double balance                  = !etBalance.getText().toString().equals("") ? Double.parseDouble(etBalance.getText().toString().replaceAll(",", "")): 0;
        Double remain                   = mDbHelper.getAccountRemainBefore(mFromAccount.getId(), mCal);

        if(remain.doubleValue() == balance.doubleValue()) {
            ((ActivityMain) getActivity()).showError(getResources().getString(R.string.Input_Error_Adjustment_Same_Amount));
            return;
        }

        int     categoryId      = mCategory != null ? mCategory.getId() : 0;
        String  description     = tvDescription.getText().toString();
        String  payee           = tvPayee.getText().toString();
        String  strEvent        = tvEvent.getText().toString();
        Event   event           = null;

        if(!strEvent.equals("")) {
            event = mDbHelper.getEventByName(strEvent);
            if(event == null) {
                long eventId = mDbHelper.createEvent(new Event(0, strEvent, mCal, null));
                if(eventId != -1) {
                    event = mDbHelper.getEvent(eventId);
                }
            }
        }

        Transaction transaction     = new Transaction(mTransaction.getId(),
                                                    TransactionEnum.Adjustment.getValue(),
                                                    Math.abs(remain - balance),
                                                    categoryId,
                                                    description,
                                                    remain > balance ? mFromAccount.getId() : 0,
                                                    remain > balance ? 0 : mFromAccount.getId(),
                                                    mCal,
                                                    0.0,
                                                    payee,
                                                    event);

        int row = mDbHelper.updateTransaction(transaction);
        if (row == 1) {
            ((ActivityMain) getActivity()).showToastSuccessful(getResources().getString(R.string.message_transaction_update_successful));
            cleanup();
        }

        LogUtils.logLeaveFunction(Tag, null, null);
        // Return to last fragment
        getFragmentManager().popBackStackImmediate();
    } // End Update Transaction

    /**
     * Show Dialog to select Time
     */
    private void showDialogTime() {
        final TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                        mCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        mCal.set(Calendar.MINUTE, minute);

                        tvDate.setText(getDateString(mCal));

                    }
                }, mCal.get(Calendar.HOUR_OF_DAY), mCal.get(Calendar.MINUTE), true);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                        mCal.set(Calendar.YEAR, year);
                        mCal.set(Calendar.MONTH, monthOfYear);
                        mCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        timePickerDialog.show();
                    }
                }, mCal.get(Calendar.YEAR), mCal.get(Calendar.MONTH), mCal.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    } // End showDialogTime

    /**
     * Start fragment to select Category
     * @param oldCategoryId
     */
    private void startFragmentSelectCategory(int oldCategoryId) {
        LogUtils.logEnterFunction(Tag, "OldCategoryId = " + oldCategoryId);
        FragmentTransactionSelectCategory nextFrag = new FragmentTransactionSelectCategory();
        Bundle bundle = new Bundle();
        bundle.putBoolean("CategoryType", isExpense);
        bundle.putInt("CategoryID", oldCategoryId);
        bundle.putSerializable("Callback", this);
        nextFrag.setArguments(bundle);
        FragmentTransactionCUDAdjustment.this.getFragmentManager().beginTransaction()
                .add(mTransaction.getId() == 0 ? R.id.ll_transaction_create : R.id.ll_transaction_update, nextFrag, FragmentTransactionSelectCategory.Tag)
                .addToBackStack(null)
                .commit();
        LogUtils.logLeaveFunction(Tag, "OldCategoryId = " + oldCategoryId, null);
    }

    /**
     * Start fragment input description
     * @param oldDescription
     */
    private void startFragmentDescription(String oldDescription) {
        FragmentDescription fragmentDescription = new FragmentDescription();
        Bundle bundle = new Bundle();
        bundle.putString("Description", oldDescription);
        bundle.putSerializable("Callback", this);
        fragmentDescription.setArguments(bundle);
        FragmentTransactionCUDAdjustment.this.getFragmentManager().beginTransaction()
                .add(mTransaction.getId() == 0 ? R.id.ll_transaction_create : R.id.ll_transaction_update, fragmentDescription, FragmentDescription.Tag)
                .addToBackStack(Tag)
                .commit();
    }

    /**
     * Start fragment to Select Account
     * @param transactionType
     * @param oldAccountId
     */
    private void startFragmentSelectAccount(TransactionEnum transactionType, int oldAccountId) {
        LogUtils.logEnterFunction(Tag, "TransactionType = " + transactionType.name() + ", oldAccountId = " + oldAccountId);
        FragmentAccountsSelect fragment = new FragmentAccountsSelect();
        Bundle bundle = new Bundle();
        bundle.putInt("AccountID", oldAccountId);
        bundle.putSerializable("TransactionType", transactionType);
        bundle.putSerializable("Callback", this);
        fragment.setArguments(bundle);
        FragmentTransactionCUDAdjustment.this.getFragmentManager().beginTransaction()
                .add(mTransaction.getId() == 0 ? R.id.ll_transaction_create : R.id.ll_transaction_update, fragment, FragmentAccountsSelect.Tag)
                .addToBackStack(null)
                .commit();

        LogUtils.logLeaveFunction(Tag, "TransactionType = " + transactionType.name() + ", oldAccountId = " + oldAccountId, null);
    }

    /**
     * Start fragment Payee
     * @param oldPayee
     */
    private void startFragmentPayee(String oldPayee) {
        FragmentPayee nextFrag = new FragmentPayee();
        Bundle bundle = new Bundle();
        bundle.putString("Payee", oldPayee);
        bundle.putSerializable("Callback", this);
        nextFrag.setArguments(bundle);
        FragmentTransactionCUDAdjustment.this.getFragmentManager().beginTransaction()
                .add(mTransaction.getId() == 0 ? R.id.ll_transaction_create : R.id.ll_transaction_update, nextFrag, "FragmentPayee")
                .addToBackStack(Tag)
                .commit();
    }

    /**
     * Start fragment Event
     * @param oldEvent
     */
    private void startFragmentEvent(String oldEvent) {
        FragmentEvent nextFrag = new FragmentEvent();
        Bundle bundle = new Bundle();
        bundle.putString("Event", oldEvent);
        bundle.putSerializable("Callback", this);
        nextFrag.setArguments(bundle);
        FragmentTransactionCUDAdjustment.this.getFragmentManager().beginTransaction()
                .add(mTransaction.getId() == 0 ? R.id.ll_transaction_create : R.id.ll_transaction_update, nextFrag, "FragmentEvent")
                .addToBackStack(Tag)
                .commit();
    }

    /**
     * Get Date's String
     * @return
     */
    private String getDateString(Calendar cal) {
        Calendar current = Calendar.getInstance();
        String date = "";
        if(cal.get(Calendar.DAY_OF_YEAR) == current.get(Calendar.DAY_OF_YEAR)) {
            date = getResources().getString(R.string.content_today) + String.format(" %02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        } else if((cal.get(Calendar.DAY_OF_YEAR) - 1) == current.get(Calendar.DAY_OF_YEAR)) {
            date = getResources().getString(R.string.content_yesterday) + String.format(" %02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        } else if((cal.get(Calendar.DAY_OF_YEAR) - 2) == current.get(Calendar.DAY_OF_YEAR)
                && getResources().getConfiguration().locale.equals(Locale.forLanguageTag("vi_VN"))) {
            date = getResources().getString(R.string.content_before_yesterday) + String.format(" %02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        } else {
            date = String.format("%02d-%02d-%02d %02d:%02d", mCal.get(Calendar.DAY_OF_MONTH), mCal.get(Calendar.MONTH) + 1, mCal.get(Calendar.YEAR), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        }

        return date;
    }

    /**
     * Cleanup Old datas
     */
    private void cleanup() {
        mCategory                   = null;
        mFromAccount                = null;
        mCal                        = null;

        /* Reset value */
        etBalance.setText("");
        tvAccount.setText("");
        tvCategory.setText("");
        tvDescription.setText("");
        tvPayee.setText("");
        tvEvent.setText("");
    }
}
