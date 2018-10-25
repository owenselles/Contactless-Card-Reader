package com.owenselles.nfcreader.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.owenselles.nfcreader.R;
import com.owenselles.nfcreader.activity.PaycardActivity;
import com.owenselles.nfcreader.object.PaycardObject;
import com.owenselles.nfcreader.util.AidUtil;
import com.owenselles.nfcreader.util.HexUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import io.realm.RealmResults;

public class PaycardItemCustomArrayAdapter extends ArrayAdapter<PaycardObject> {
    private LayoutInflater mLayoutInflater;

    public PaycardItemCustomArrayAdapter(@NonNull Context context, int resource, @NonNull RealmResults<PaycardObject> paycardObjectRealmResults) {
        super(context, resource, paycardObjectRealmResults);

        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final ViewHolder viewHolder;

        final PaycardObject paycardObject = getItem(position);

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_paycard, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.paycardPanTextView = convertView.findViewById(R.id.item_paycard_pan);
            viewHolder.paycardTypeImageView = convertView.findViewById(R.id.item_paycard_type_image);
            viewHolder.paycardAidTextView = convertView.findViewById(R.id.item_paycard_aid);
            viewHolder.paycardTypeTextView = convertView.findViewById(R.id.item_paycard_type);
            viewHolder.paycardExpDateTextView = convertView.findViewById(R.id.item_paycard_exp_date);
            viewHolder.paycardAddDateTextView = convertView.findViewById(R.id.item_paycard_add_date);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), PaycardActivity.class);
                if (paycardObject != null) {
                    intent.putExtra(view.getContext().getString(R.string.pan_var_name), paycardObject.getApplicationPan());
                }

                view.getContext().startActivity(intent);
            }
        });

        if (paycardObject != null) {
            // PAN
            byte[] applicationPan = paycardObject.getApplicationPan();

            String applicationPanHexadecimal = HexUtil.bytesToHexadecimal(applicationPan);

            String panPreview = null; // Hide the last 4 characters (digits) from the preview (which are part from the unique ones) because of safety reasons
            if (applicationPanHexadecimal != null) {
                if (applicationPanHexadecimal.length() > (16 / 4)) {
                    panPreview = applicationPanHexadecimal.substring(0, applicationPanHexadecimal.length() - (16 / 4));
                    panPreview += "****";
                } else {
                    panPreview = applicationPanHexadecimal;
                }

                panPreview = panPreview.replaceAll( "....(?!$)", "$0 "); // Avoiding the final (extra) space
            }

            viewHolder.paycardPanTextView.setText(panPreview != null ? panPreview : "N/A");
            // - PAN

            // Type (image)
            if (Arrays.equals(paycardObject.getAid(), AidUtil.A0000000041010)) {
                viewHolder.paycardTypeImageView.setImageResource(R.drawable.icon_mastercard);
            } else if (Arrays.equals(paycardObject.getAid(), AidUtil.A0000000043060)) {
                viewHolder.paycardTypeImageView.setImageResource(R.drawable.icon_maestro);
            } else if (Arrays.equals(paycardObject.getAid(), AidUtil.A0000000031010)) {
                viewHolder.paycardTypeImageView.setImageResource(R.drawable.icon_visa);
            } else if (Arrays.equals(paycardObject.getAid(), AidUtil.A0000000032010)) {
                viewHolder.paycardTypeImageView.setImageResource(R.drawable.icon_visa_electron);
            } else {
                // TODO: Default paycard image
            }
            // - Type (image)

            // AID
            String aidString = HexUtil.bytesToHexadecimal(paycardObject.getAid());

            viewHolder.paycardAidTextView.setText("AID: " + (aidString != null ? aidString : "N/A"));
            // - AID

            // Type (text)
            if (Arrays.equals(paycardObject.getAid(), AidUtil.A0000000041010)) {
                viewHolder.paycardTypeTextView.setText(convertView.getContext().getString(R.string.mastercard));
            } else if (Arrays.equals(paycardObject.getAid(), AidUtil.A0000000043060)) {
                viewHolder.paycardTypeTextView.setText(convertView.getContext().getString(R.string.maestro));
            } else if (Arrays.equals(paycardObject.getAid(), AidUtil.A0000000031010)) {
                viewHolder.paycardTypeTextView.setText(convertView.getContext().getString(R.string.visa));
            } else if (Arrays.equals(paycardObject.getAid(), AidUtil.A0000000032010)) {
                viewHolder.paycardTypeTextView.setText(convertView.getContext().getString(R.string.visa_electron));
            } else {
                viewHolder.paycardTypeTextView.setText("Type: N/A");
            }
            // - Type (text)

            // Exp Date
            String expDateString = null;

            Date expDate = null;
            try {
                expDate = new SimpleDateFormat("yyMMdd", Locale.getDefault()).parse(HexUtil.bytesToHexadecimal(paycardObject.getApplicationExpirationDate()));
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println(e.toString());

                e.printStackTrace();
            }

            if (expDate != null) {
                try {
                    expDateString = new SimpleDateFormat("MM/yy", Locale.getDefault()).format(expDate);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println(e.toString());

                    e.printStackTrace();
                }
            }

            viewHolder.paycardExpDateTextView.setText("Exp. Date: " + (expDateString != null ? expDateString : "N/A"));
            // - Exp Date

            // Add Date
            String addDateString = null;

            Date addDate = null;
            try {
                addDate = paycardObject.getAddDate();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println(e.toString());

                e.printStackTrace();
            }

            if (addDate != null) {
                if (!android.text.format.DateFormat.is24HourFormat(convertView.getContext())) {
                    try {
                        addDateString = new SimpleDateFormat("hh:mm:ss a dd/MM/yy", Locale.getDefault()).format(addDate);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        System.out.println(e.toString());

                        e.printStackTrace();
                    }
                } else {
                    try {
                        addDateString = new SimpleDateFormat("HH:mm:ss dd/MM/yy", Locale.getDefault()).format(addDate);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        System.out.println(e.toString());

                        e.printStackTrace();
                    }
                }

                viewHolder.paycardAddDateTextView.setText(addDateString != null ? addDateString : "N/A");
            }
            // - Add Date
        } else {
            viewHolder.paycardPanTextView.setText("N/A"); // PAN

            // TODO: Default paycard image // Type (image)

            viewHolder.paycardAidTextView.setText("AID: N/A"); // AID

            viewHolder.paycardTypeTextView.setText("Type: N/A"); // Type (text)

            viewHolder.paycardExpDateTextView.setText("Exp. Date: N/A"); // Exp Date

            viewHolder.paycardAddDateTextView.setText("Added / Updated: N/A"); // Add Date
        }

        return convertView;
    }

    private class ViewHolder {
        TextView paycardPanTextView = null;
        ImageView paycardTypeImageView = null;
        TextView paycardAidTextView = null;
        TextView paycardTypeTextView = null;
        TextView paycardExpDateTextView = null;
        TextView paycardAddDateTextView = null;
    }
}
