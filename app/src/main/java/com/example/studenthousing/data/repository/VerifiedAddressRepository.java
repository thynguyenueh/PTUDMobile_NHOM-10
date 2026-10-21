package com.example.studenthousing.data.repository;

import android.content.Context;

import com.example.studenthousing.data.local.VerifiedAddressDao;
import com.example.studenthousing.data.model.VerifiedAddress;
import com.example.studenthousing.data.remote.VerifiedAddressRemote;
import com.example.studenthousing.util.AddressFingerprintUtil;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.NetworkUtils;

import java.util.List;

public class VerifiedAddressRepository {
    private final Context ctx;
    private final VerifiedAddressDao local;
    private final VerifiedAddressRemote remote;

    public VerifiedAddressRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.local = new VerifiedAddressDao(this.ctx);
        this.remote = new VerifiedAddressRemote();
    }

    public void claimAddress(int cityCode, int wardCode, String street, String houseNumber, String roomNumber,
                             String fullAddress, String userId, String ticketId, AppCallback<VerifiedAddress> cb) {
        String fingerprint = AddressFingerprintUtil.build(cityCode, wardCode, street, houseNumber, roomNumber);

        VerifiedAddress addr = new VerifiedAddress();
        addr.setAddressId(fingerprint);
        addr.setFingerprint(fingerprint);
        addr.setCityCode(cityCode);
        addr.setWardCode(wardCode);
        addr.setStreetNormalized(AddressFingerprintUtil.normalize(street));
        addr.setHouseNumberNorm(AddressFingerprintUtil.normalize(houseNumber));
        addr.setRoomNumberNorm(AddressFingerprintUtil.normalize(roomNumber));
        addr.setFullAddress(fullAddress);
        addr.setOwnerUserId(userId);
        addr.setVerificationTicketId(ticketId);
        addr.setVerifiedAt(System.currentTimeMillis());
        addr.setStatus(Constants.ADDR_VERIFIED);
        addr.setCachedAt(System.currentTimeMillis());

        local.upsert(addr);

        if (NetworkUtils.isOnline(ctx)) {
            remote.save(addr, new AppCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    if (cb != null) cb.onSuccess(addr);
                }

                @Override
                public void onError(Exception e) {
                    if (cb != null) cb.onSuccess(addr);
                }
            });
        } else {
            if (cb != null) cb.onSuccess(addr);
        }
    }

    public void findByFingerprint(String fingerprint, AppCallback<VerifiedAddress> cb) {
        VerifiedAddress cached = local.findByFingerprint(fingerprint);
        if (cached != null) {
            if (cb != null) cb.onSuccess(cached);
            return;
        }

        if (NetworkUtils.isOnline(ctx)) {
            remote.getByFingerprint(fingerprint, new AppCallback<VerifiedAddress>() {
                @Override
                public void onSuccess(VerifiedAddress remoteAddr) {
                    if (remoteAddr != null) {
                        local.upsert(remoteAddr);
                    }
                    if (cb != null) cb.onSuccess(remoteAddr);
                }

                @Override
                public void onError(Exception e) {
                    if (cb != null) cb.onError(e);
                }
            });
        } else {
            if (cb != null) cb.onSuccess(null);
        }
    }

    public void getByOwner(String ownerUserId, AppCallback<List<VerifiedAddress>> cb) {
        List<VerifiedAddress> cached = local.getByOwner(ownerUserId);
        if (cb != null) cb.onSuccess(cached);

        if (NetworkUtils.isOnline(ctx)) {
            remote.getByOwner(ownerUserId, new AppCallback<List<VerifiedAddress>>() {
                @Override
                public void onSuccess(List<VerifiedAddress> remoteList) {
                    for (VerifiedAddress a : remoteList) {
                        local.upsert(a);
                    }
                    if (cb != null) cb.onSuccess(remoteList);
                }

                @Override
                public void onError(Exception e) {
                }
            });
        }
    }
}
