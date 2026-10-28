package com.example.studenthousing.util;

import android.content.Context;

import com.example.studenthousing.data.model.province.Province;
import com.example.studenthousing.data.model.province.Ward;
import com.example.studenthousing.ui.dialog.SearchListDialog;

import java.util.ArrayList;
import java.util.List;

public class ProvincePickerHelper {

    public interface OnAddressSelectedListener {
        void onAddressSelected(Province province, Ward ward);
    }

    public static void showPicker(Context context, OnAddressSelectedListener listener) {
        Province hcm = new Province(79, "TP. Hồ Chí Minh");
        List<Ward> wards = buildWardList();

        if (wards == null || wards.isEmpty()) {
            if (listener != null) {
                listener.onAddressSelected(hcm, null);
            }
            return;
        }

        String[] wardNames = new String[wards.size()];
        for (int i = 0; i < wards.size(); i++) {
            wardNames[i] = wards.get(i).getName();
        }

        SearchListDialog.show(context, "Chọn Phường / Xã (TP. Hồ Chí Minh)", wardNames, selectedName -> {
            if (listener != null) {
                Ward selectedWard = null;
                for (Ward w : wards) {
                    if (w.getName().equals(selectedName)) {
                        selectedWard = w;
                        break;
                    }
                }
                listener.onAddressSelected(hcm, selectedWard);
            }
        });
    }

    private static List<Ward> buildWardList() {
        List<Ward> wards = new ArrayList<>();
        int code = 27256;
        for (String wardName : LocationData.WARDS) {
            wards.add(new Ward(code++, wardName));
        }
        return wards;
    }
}
