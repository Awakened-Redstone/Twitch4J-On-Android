package com.example.t4jandroid;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.t4jandroid.databinding.ActivityMainBinding;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.ChatConnectionStateEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.util.EventManagerUtils;
import com.github.twitch4j.graphql.TwitchGraphQLBuilder;
import com.github.twitch4j.graphql.command.CommandFetchChatters;
import com.github.twitch4j.graphql.internal.FetchChattersQuery;
import com.google.android.material.snackbar.Snackbar;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public static TwitchClient twitchClient;
    ArrayList<String> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        createNotificationChannel();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        EventManager em = EventManagerUtils.initializeEventManager(SimpleEventHandler.class);
        em.onEvent(ChatConnectionStateEvent.class, System.out::println);

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);

        Log.i("A101", "Twitch4J starting");
        twitchClient = TwitchClientBuilder.builder()
                .withEnableGraphQL(true)
                .withEnableChat(true)
                //.withChatServer("ws://irc-ws.chat.twitch.tv:80")
                //.withEnableHelix(true)
                //.withEnablePubSub(true)
                .build();
        Log.i("A101", "Twitch4J started");

        binding.fab.setOnClickListener(view -> {
            //if (twitchClient == null) Log.e("A101", "twitchClient is null!");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1")
                    .setSmallIcon(R.drawable.ic_logo_2020)
                    .setContentTitle("title")
                    .setContentText("content")
                    .setColor(0x8205b4)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Much longer text that cannot fit one line, I think, idk, let me just " +
                                    "make this text bigger so it is for sure bigger than one line..."))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, builder.build());

            MainActivity.twitchClient.getChat().getEventManager().onEvent(ChannelMessageEvent.class, this::onMessage);
            twitchClient.getChat().joinChannel("shylily");

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this.getBaseContext(), R.layout.list_item, list);
            binding.listView.setAdapter(adapter);

            CommandFetchChatters fetchChatters = TwitchGraphQLBuilder.builder().build()
                    .fetchChatters(null, "shylily");
            FetchChattersQuery.Data data = fetchChatters.execute();
            FetchChattersQuery.Chatters chatters = data.channel().chatters();

            //TwitchHelixBuilder.builder().build();

            String text = getString(R.string.snack_bar,
                    String.valueOf(chatters.count()),
                    String.valueOf(chatters.moderators().size()));

            /*HystrixCommand<UserList> users = twitchClient.getHelix().getUsers(null, Collections.emptyList(), Collections.singletonList("captainsparklez"));
            String text = users.execute().getUsers().get(0).getDisplayName();*/

            Log.i("A101", text);

            Snackbar.make(view, text, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });
    }

    public void onMessage(ChannelMessageEvent event) {
        this.runOnUiThread(() -> {
            list.add(getString(R.string.chat_message, event.getUser().getName(), event.getMessage()));
            ((ArrayAdapter<String>) binding.listView.getAdapter()).notifyDataSetChanged();
            binding.listView.post(() -> binding.listView.setSelection(binding.listView.getAdapter().getCount() - 1));
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("1", name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            //Navigation.findNavController(this, R.id.nav_host_fragment_content_settings).navigate(R.id.action_FirstFragment_to_SecondFragment);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}