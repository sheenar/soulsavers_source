package com.creatrove.soulsavers;

import android.location.Address;
import android.location.Geocoder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class NotificationActivity extends ActionBarActivity {

    protected static final String TAG = "SOULSAVERS_LOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Log.d(TAG, "NotificationActivity Oncreate");

        String message =  getIntent().getStringExtra("title_notification");
        String location[] = new String[2];
        int i=0;
        if(message != null)
        {
            for (String retval: message.split("@ ", 2)){
                if(retval.contains(";")) {
                    i=0;
                    for (String retval2:
                            retval.split(";", 2)){
                        System.out.println
                                (retval2);
                        location[i] = retval2;
                        i++;
                    }
                }
                else
                    continue;
            }

           /*Get the Address from the lat and long co-ordinates*/
            if(!(location[0].equals("unknown")) && !location[1].equals("Unkown")) {
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                try {
                    List<Address> listAddresses = geocoder.getFromLocation(Double.parseDouble(location[0]), Double.parseDouble(location[1]), 1);
                    if (null != listAddresses && listAddresses.size() > 0) {
                        String _location = listAddresses.get(0).getAddressLine(0)+", "+listAddresses.get(0).getAddressLine(0)+", "
                                +listAddresses.get(0).getLocality()+", Pin - "+listAddresses.get(0).getPostalCode();

                        Toast.makeText(getApplicationContext(),
                                "location of SOS is " /*+ location[0] + " and " + location[1]*/ + _location,
                                Toast.LENGTH_LONG).show();
                        ((TextView)findViewById(R.id.notification_activity_text))
                                .setText("location of SOS is " /*+ location[0] + \" and \" + location[1]*/ + _location);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                Toast.makeText(getApplicationContext(),
                        "location of SOS is "+location[0]+" and "+location[1],
                        Toast.LENGTH_LONG).show();
                ((TextView)findViewById(R.id.notification_activity_text))
                        .setText("location of SOS is not known :(");

            }


        }
        else
        {
            ((TextView)findViewById(R.id.notification_activity_text)).setText("SOS Message Details not available...:-(");
        }
    }



}
