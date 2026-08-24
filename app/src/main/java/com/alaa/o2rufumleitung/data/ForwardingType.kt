package com.alaa.o2rufumleitung.data

import androidx.annotation.StringRes
import com.alaa.o2rufumleitung.R

/**
 * The five GSM call-forwarding categories, identified by their standard
 * ETSI/3GPP MMI (USSD) control codes. These are part of the GSM standard,
 * so they work identically on any network, including o2 Germany:
 *
 *   activate:   **<code>*<number>#
 *   deactivate: ##<code>#
 *   check:      *#<code>#
 */
enum class ForwardingType(
    val mmiCode: String,
    val deactivationCode: String,
    val statusCode: String,
    @StringRes val titleRes: Int,
    @StringRes val explanationRes: Int
) {
    UNCONDITIONAL(
        mmiCode = "21",
        deactivationCode = "##21#",
        statusCode = "*#21#",
        titleRes = R.string.type_unconditional_title,
        explanationRes = R.string.type_unconditional_explanation
    ),
    BUSY(
        mmiCode = "67",
        deactivationCode = "##67#",
        statusCode = "*#67#",
        titleRes = R.string.type_busy_title,
        explanationRes = R.string.type_busy_explanation
    ),
    NO_ANSWER(
        mmiCode = "61",
        deactivationCode = "##61#",
        statusCode = "*#61#",
        titleRes = R.string.type_no_answer_title,
        explanationRes = R.string.type_no_answer_explanation
    ),
    UNREACHABLE(
        mmiCode = "62",
        deactivationCode = "##62#",
        statusCode = "*#62#",
        titleRes = R.string.type_unreachable_title,
        explanationRes = R.string.type_unreachable_explanation
    ),
    ALL_CONDITIONAL(
        mmiCode = "004",
        deactivationCode = "##004#",
        statusCode = "*#004#",
        titleRes = R.string.type_all_conditional_title,
        explanationRes = R.string.type_all_conditional_explanation
    );

    /** Builds the activation code for a given destination number, e.g. **21*+491701234567# */
    fun activationCode(number: String): String = "**$mmiCode*$number#"
}
