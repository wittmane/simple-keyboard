package android.text;

public interface TextWatcher extends NoCopySpan {
    void beforeTextChanged(CharSequence s, int start,
                                  int count, int after);
    void onTextChanged(CharSequence s, int start, int before, int count);
    void afterTextChanged(Editable s);
}
