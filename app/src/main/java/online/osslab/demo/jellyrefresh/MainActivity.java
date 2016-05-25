package online.osslab.demo.jellyrefresh;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import online.osslab.JellyRefreshLayout;


/**
 * 动感刷新
 * http://jellyrefresh.osslab.online
 */

public class MainActivity extends AppCompatActivity {

    private JellyRefreshLayout rippleRefreshLayout;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ripple);

        rippleRefreshLayout = (JellyRefreshLayout) findViewById(R.id.refreshLayout);
        listView = (ListView) findViewById(R.id.listView);

        String[] str = {
                "",
                "",
                "",
                "",
                "我狂野不屈",
                "因此，我总是和我所在的",
                "世界发生冲突",
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.view_text_item, str);
        listView.setAdapter(adapter);

        rippleRefreshLayout.setOnRefreshListener(
                new JellyRefreshLayout.OnJellyRefreshListener() {
                    @Override
                    public void refreshing() {
                        // do something when refresh starts
                        makeToast(MainActivity.this, "正在更新...");

                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                rippleRefreshLayout.finishRefreshing();
                            }

                        }, 3000);
                    }

                    @Override
                    public void completeRefresh() {
                        // do something when refresh complete
                        makeToast(MainActivity.this, "更新成功");
                    }
                });

    }

    static void makeToast(Context context, String string) {
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }
}
