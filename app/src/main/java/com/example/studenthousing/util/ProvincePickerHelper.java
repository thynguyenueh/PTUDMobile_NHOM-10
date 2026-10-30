package com.example.studenthousing.util;

import android.content.Context;

import com.example.studenthousing.data.loader.WardsDataLoader;
import com.example.studenthousing.data.model.Province;
import com.example.studenthousing.data.model.Ward;
import com.example.studenthousing.ui.dialog.SearchListDialog;

import java.util.List;

public class ProvincePickerHelper {

    public interface OnAddressSelectedListener {
        void onAddressSelected(Province province, Ward ward);
    }

    public static void showPicker(Context context, OnAddressSelectedListener listener) {
        Province hcm = new Province(79, "TP. Hồ Chí Minh");
        List<Ward> wards = buildWardList(context);

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

    private static List<Ward> buildWardList(Context context) {
        return WardsDataLoader.loadWards(context);
    }
}
