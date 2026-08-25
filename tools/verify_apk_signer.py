#!/usr/bin/env python3
"""Print the Android APK v2/v3 signer certificate SHA-256 fingerprint.

This lightweight verifier reads the APK Signing Block directly and does not
require Android SDK build tools. Use it to compare the signer identity of two
published APKs before claiming they support an in-place update.
"""

from __future__ import annotations

import argparse
import hashlib
import struct
from pathlib import Path

from cryptography import x509
from cryptography.hazmat.primitives import hashes

APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"
APK_SIGNATURE_SCHEME_V2_ID = 0x7109871A
APK_SIGNATURE_SCHEME_V3_ID = 0xF05368C0


def read_length_prefixed(data: bytes, offset: int) -> tuple[bytes, int]:
    if offset + 4 > len(data):
        raise ValueError("Missing length prefix")
    length = struct.unpack_from("<I", data, offset)[0]
    offset += 4
    end = offset + length
    if end > len(data):
        raise ValueError("Invalid length-prefixed field")
    return data[offset:end], end


def signature_pairs(apk: Path) -> dict[int, bytes]:
    with apk.open("rb") as file:
        file.seek(0, 2)
        file_size = file.tell()
        if file_size < 32:
            raise ValueError("File is too small to be an APK")

        # The signing block sits immediately before the ZIP central directory,
        # not at the physical end of the APK. Locate the ZIP End Of Central
        # Directory record first, then use its central-directory offset.
        search_start = max(0, file_size - 65_557)
        file.seek(search_start)
        tail = file.read()
        eocd_relative = tail.rfind(b"PK\x05\x06")
        if eocd_relative < 0 or eocd_relative + 22 > len(tail):
            raise ValueError("ZIP end-of-central-directory record not found")
        comment_length = struct.unpack_from("<H", tail, eocd_relative + 20)[0]
        if eocd_relative + 22 + comment_length != len(tail):
            raise ValueError("Invalid ZIP end-of-central-directory record")
        central_directory_offset = struct.unpack_from("<I", tail, eocd_relative + 16)[0]
        if central_directory_offset == 0xFFFFFFFF:
            raise ValueError("ZIP64 APKs are not supported by this verifier")
        if central_directory_offset < 24:
            raise ValueError("APK has no room for a signing block")

        file.seek(central_directory_offset - 24)
        footer = file.read(24)
        block_size = struct.unpack_from("<Q", footer, 0)[0]
        if footer[8:] != APK_SIG_BLOCK_MAGIC:
            raise ValueError("APK Signing Block not found")
        block_start = central_directory_offset - (block_size + 8)
        if block_start < 0:
            raise ValueError("Invalid APK Signing Block size")
        file.seek(block_start)
        if struct.unpack("<Q", file.read(8))[0] != block_size:
            raise ValueError("APK Signing Block size mismatch")
        pairs_data = file.read(block_size - 24)

    pairs: dict[int, bytes] = {}
    offset = 0
    while offset < len(pairs_data):
        if offset + 8 > len(pairs_data):
            raise ValueError("Truncated signing pair")
        pair_size = struct.unpack_from("<Q", pairs_data, offset)[0]
        offset += 8
        if pair_size < 4 or offset + pair_size > len(pairs_data):
            raise ValueError("Invalid signing pair size")
        pair_id = struct.unpack_from("<I", pairs_data, offset)[0]
        pairs[pair_id] = pairs_data[offset + 4 : offset + pair_size]
        offset += pair_size
    return pairs


def signer_certificate_der(signers_data: bytes) -> bytes:
    signers, _ = read_length_prefixed(signers_data, 0)
    signer, _ = read_length_prefixed(signers, 0)
    signed_data, _ = read_length_prefixed(signer, 0)
    _, offset = read_length_prefixed(signed_data, 0)  # digests
    certificates, _ = read_length_prefixed(signed_data, offset)
    certificate_der, _ = read_length_prefixed(certificates, 0)
    return certificate_der


def fingerprint(apk: Path) -> str:
    pairs = signature_pairs(apk)
    signing_block = pairs.get(APK_SIGNATURE_SCHEME_V3_ID) or pairs.get(APK_SIGNATURE_SCHEME_V2_ID)
    if signing_block is None:
        raise ValueError("No APK Signature Scheme v2/v3 signer found")
    certificate = x509.load_der_x509_certificate(signer_certificate_der(signing_block))
    return certificate.fingerprint(hashes.SHA256()).hex().upper()


def main() -> None:
    parser = argparse.ArgumentParser(description="Print APK signer certificate fingerprint")
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    digest = fingerprint(args.apk)
    print(f"{args.apk.name}: SHA-256 certificate fingerprint: {digest}")
    print(f"{args.apk.name}: SHA-256 certificate DER: {hashlib.sha256(signer_certificate_der(signature_pairs(args.apk).get(APK_SIGNATURE_SCHEME_V3_ID) or signature_pairs(args.apk)[APK_SIGNATURE_SCHEME_V2_ID])).hexdigest().upper()}")


if __name__ == "__main__":
    main()
