package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DataCheckActivity extends AppCompatActivity {
    ListView listView;
    DataAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        listView = (ListView) findViewById(R.id.listView);

        adapter = new DataAdapter();

        adapter.addItem(new DtItem("220523", "15:25:10"));
        adapter.addItem(new DtItem("220518", "10:12:30"));
        adapter.addItem(new DtItem("220523", "15:25:10"));
        adapter.addItem(new DtItem("220518", "10:12:30"));
        adapter.addItem(new DtItem("220523", "15:25:10"));
        adapter.addItem(new DtItem("220518", "10:12:30"));

        listView.setAdapter(adapter);

        adapter.notifyDataSetChanged();

        Intent intent1 = new Intent(getApplicationContext(), InfoActivity1.class);
        Intent intent2 = new Intent(getApplicationContext(), InfoActivity2.class);
        Intent intent3 = new Intent(getApplicationContext(), InfoActivity3.class);
        Intent intent4 = new Intent(getApplicationContext(), InfoActivity4.class);
        Intent intent5 = new Intent(getApplicationContext(), InfoActivity5.class);
        Intent intent6 = new Intent(getApplicationContext(), InfoActivity6.class);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position+1) {
                    case (2):
                        intent2.putExtra("num", position+1);
                        Log.e("position", String.valueOf(position));
                        startActivity(intent2);
                        break;
                    case (3):
                        intent3.putExtra("num", position+1);
                        Log.e("position", String.valueOf(position));
                        startActivity(intent3);
                        break;
                    case (4):
                        intent4.putExtra("num", position+1);
                        Log.e("position", String.valueOf(position));
                        startActivity(intent4);
                        break;
                    case (5):
                        intent5.putExtra("num", position+1);
                        Log.e("position", String.valueOf(position));
                        startActivity(intent5);
                        break;
                    case (6):
                        intent6.putExtra("num", position+1);
                        Log.e("position", String.valueOf(position));
                        startActivity(intent6);
                        break;
                    default:
                        intent1.putExtra("num", position+1);
                        Log.e("position", String.valueOf(position));
                        startActivity(intent1);
                        break;
                }

                DtItem item = (DtItem) adapter.getItem(position);
                Toast.makeText(getApplicationContext(), "선택 : " + item.getTimeName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    class DataAdapter extends BaseAdapter {
        private ArrayList<DtItem> items = new ArrayList<DtItem>();

        public void addItem(DtItem item) {
            items.add(item);
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DtItemView view = new DtItemView(getApplicationContext());

            DtItem item = items.get(position);

            view.setTextTime(item.getTimeName());
            view.setTextDate(item.getDateName());

            return view;
        }
    }
}
