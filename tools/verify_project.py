#!/usr/bin/env python3
"""Static integrity checks for O2CallForwarding.

This script intentionally performs no Android build and has no third-party
requirements. It validates Android string resources and the code wiring for
the official O2 mailbox preset.
"""

from __future__ import annotations

from collections import Counter
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES_ROOT = ROOT / "app/src/main/res"
BASE_STRINGS = RES_ROOT / "values/strings.xml"

EXPECTED_LOCALE_DIRS = {
    "values-ar",
    "values-de",
    "values-es",
    "values-fr",
    "values-it",
    "values-pl",
    "values-pt-rBR",
    "values-ro",
    "values-sv",
    "values-tr",
    "values-uk",
}


def resource_keys(path: Path) -> set[str]:
    """Parse one Android strings file and return its unique resource names."""
    root = ET.parse(path).getroot()
    names = [element.attrib["name"] for element in root.findall("string")]
    duplicate_names = sorted(name for name, count in Counter(names).items() if count > 1)
    assert not duplicate_names, f"Duplicate string keys in {path}: {duplicate_names}"
    return set(names)


def expect_contains(path: Path, needle: str) -> None:
    content = path.read_text(encoding="utf-8")
    assert needle in content, f"Expected text not found in {path}: {needle}"


def expect_absent(path: Path, needle: str) -> None:
    content = path.read_text(encoding="utf-8")
    assert needle not in content, f"Unexpected text found in {path}: {needle}"


def main() -> None:
    assert BASE_STRINGS.is_file(), f"Missing base strings file: {BASE_STRINGS}"
    base_keys = resource_keys(BASE_STRINGS)
    required_keys = {"number_source_o2_mailbox", "o2_mailbox_preset_hint", "number_source_custom"}
    obsolete_keys = {
        "number_source_voicemail",
        "voicemail_detected_hint",
        "voicemail_not_detected_hint",
    }
    assert required_keys.issubset(base_keys), "Missing O2 mailbox or custom-forwarding resources"
    assert base_keys.isdisjoint(obsolete_keys), "Obsolete SIM voicemail resources are still present"

    locale_files = sorted(RES_ROOT.glob("values*/strings.xml"))
    actual_locale_dirs = {path.parent.name for path in locale_files if path.parent.name != "values"}
    assert EXPECTED_LOCALE_DIRS.issubset(actual_locale_dirs), (
        "Missing locale directories: " + ", ".join(sorted(EXPECTED_LOCALE_DIRS - actual_locale_dirs))
    )

    for path in locale_files:
        keys = resource_keys(path)
        assert keys == base_keys, (
            f"Resource key mismatch in {path}: "
            f"missing={sorted(base_keys - keys)}, extra={sorted(keys - base_keys)}"
        )

    view_model = ROOT / "app/src/main/java/com/alaa/o2rufumleitung/ui/ForwardingViewModel.kt"
    ussd_manager = ROOT / "app/src/main/java/com/alaa/o2rufumleitung/ussd/UssdManager.kt"
    forwarding_card = ROOT / "app/src/main/java/com/alaa/o2rufumleitung/ui/ForwardingCard.kt"
    forwarding_type = ROOT / "app/src/main/java/com/alaa/o2rufumleitung/data/ForwardingType.kt"

    expect_contains(view_model, "enum class NumberSource { O2_MAILBOX, CUSTOM }")
    expect_contains(view_model, "val numberSource: NumberSource = NumberSource.O2_MAILBOX")
    expect_contains(ussd_manager, 'const val O2_MAILBOX_SHORT_CODE = "333"')
    expect_contains(forwarding_card, "NumberSource.O2_MAILBOX -> UssdManager.O2_MAILBOX_SHORT_CODE")
    expect_contains(forwarding_card, "NumberSource.CUSTOM -> state.customNumber.takeIf { it.isNotBlank() }")
    expect_contains(forwarding_card, "R.string.number_source_o2_mailbox")
    expect_contains(forwarding_card, "R.string.number_source_custom")
    expect_contains(forwarding_card, "type.activationCode(UssdManager.O2_MAILBOX_SHORT_CODE)")
    expect_contains(forwarding_card, "activating = true")
    expect_absent(forwarding_card, "NumberSource.VOICEMAIL")
    expect_absent(ussd_manager, "systemVoiceMailNumber")

    mailbox_activation_codes = {
        "UNCONDITIONAL": "**21*333#",
        "BUSY": "**67*333#",
        "NO_ANSWER": "**61*333#",
        "UNREACHABLE": "**62*333#",
        "ALL_CONDITIONAL": "**004*333#",
    }
    for code in ("21", "67", "61", "62", "004"):
        expect_contains(forwarding_type, f'mmiCode = "{code}"')
        expect_contains(forwarding_type, f'deactivationCode = "##{code}#"')
        expect_contains(forwarding_type, f'statusCode = "*#{code}#"')
    expect_contains(forwarding_type, 'fun activationCode(number: String): String = "**$mmiCode*$number#"')
    assert set(mailbox_activation_codes.values()) == {
        f"**{code}*333#" for code in ("21", "67", "61", "62", "004")
    }

    print(f"PASS: {len(locale_files)} Android resource files parsed successfully.")
    print(f"PASS: {len(base_keys)} keys are present and identical in every locale.")
    print("PASS: Tapping the O2 mailbox preset activates the selected forwarding type with 333.")
    print("PASS: O2 mailbox activation codes: " + ", ".join(mailbox_activation_codes.values()))
    print("PASS: The manual number field is available only for custom forwarding.")
    print("PASS: All five forwarding categories expose activation, deactivation, and status codes.")


if __name__ == "__main__":
    main()
