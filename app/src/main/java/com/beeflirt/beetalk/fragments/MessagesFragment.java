package com.beeflirt.beetalk.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.activities.ChatActivity;
import com.beeflirt.beetalk.adapters.ConversationsAdapter;
import com.beeflirt.beetalk.adapters.NewMatchesAdapter;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.Conversation;
import com.beeflirt.beetalk.models.Match;
import com.beeflirt.beetalk.repository.ChatRepository;
import com.beeflirt.beetalk.repository.MatchRepository;
import com.beeflirt.beetalk.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment implements
        ConversationsAdapter.OnConversationClickListener,
        NewMatchesAdapter.OnNewMatchClickListener {

    private RecyclerView recyclerView;
    private RecyclerView rvNewMatches;
    private ConversationsAdapter adapter;
    private NewMatchesAdapter newMatchesAdapter;
    private List<Conversation> conversations;
    private List<Conversation> allConversations;
    private List<Match> newMatches;
    private ProgressBar progressBar;
    private LinearLayout tvNoConversations;
    private LinearLayout sectionNewMatches;
    private TextView tvNewMatchesCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearch;

    private ApiService apiService;
    private ChatRepository chatRepository;
    private MatchRepository matchRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        apiService = ApiClient.getApiService(requireContext());
        chatRepository = new ChatRepository(requireContext());
        matchRepository = new MatchRepository(requireContext());

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view);
        rvNewMatches = view.findViewById(R.id.rv_new_matches);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoConversations = view.findViewById(R.id.tv_no_conversations);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        sectionNewMatches = view.findViewById(R.id.section_new_matches);
        tvNewMatchesCount = view.findViewById(R.id.tv_new_matches_count);
        etSearch = view.findViewById(R.id.et_search);

        // Initialize lists
        conversations = new ArrayList<>();
        allConversations = new ArrayList<>();
        newMatches = new ArrayList<>();

        // Setup RecyclerViews
        setupRecyclerView();
        setupNewMatchesRecyclerView();

        // Setup search
        setupSearch();

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        // Load data
        loadData();

        // Initialize and load banner ad
        RelativeLayout bannerContainer = view.findViewById(R.id.banner_container);
        Ads.ShowBanner(getActivity(), bannerContainer, com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_BANNER_MESSAGES);

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ConversationsAdapter(conversations, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupNewMatchesRecyclerView() {
        newMatchesAdapter = new NewMatchesAdapter(newMatches, this);
        rvNewMatches.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvNewMatches.setAdapter(newMatchesAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterConversations(String query) {
        if (query.isEmpty()) {
            conversations.clear();
            conversations.addAll(allConversations);
        } else {
            conversations.clear();
            String lowerQuery = query.toLowerCase();
            for (Conversation c : allConversations) {
                if (c.getName() != null && c.getName().toLowerCase().contains(lowerQuery)) {
                    conversations.add(c);
                }
            }
        }
        adapter.updateConversations(conversations);

        if (conversations.isEmpty() && !allConversations.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvNoConversations.setVisibility(View.VISIBLE);
        } else if (!conversations.isEmpty()) {
            recyclerView.setVisibility(View.VISIBLE);
            tvNoConversations.setVisibility(View.GONE);
        }
    }

    private void loadData() {
        loadConversations();
        loadNewMatches();
    }

    private void loadConversations() {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        tvNoConversations.setVisibility(View.GONE);

        chatRepository.getConversations(new RepositoryCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> newConversations) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (newConversations != null && !newConversations.isEmpty()) {
                    allConversations.clear();
                    allConversations.addAll(newConversations);
                    conversations.clear();
                    conversations.addAll(newConversations);
                    adapter.updateConversations(conversations);

                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoConversations.setVisibility(View.GONE);
                } else {
                    showNoConversations();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                showNoConversations();
            }
        });
    }

    private void loadNewMatches() {
        matchRepository.getMatches(new RepositoryCallback<List<Match>>() {
            @Override
            public void onSuccess(List<Match> allMatches) {
                if (!isAdded()) return;

                if (allMatches != null) {
                    newMatches.clear();
                    for (Match match : allMatches) {
                        if (match.getLastMessage() == null || match.getLastMessage().isEmpty()) {
                            newMatches.add(match);
                        }
                    }

                    if (!newMatches.isEmpty()) {
                        sectionNewMatches.setVisibility(View.VISIBLE);
                        tvNewMatchesCount.setText(newMatches.size() + " new");
                        newMatchesAdapter.updateMatches(newMatches);
                    } else {
                        sectionNewMatches.setVisibility(View.GONE);
                    }
                } else {
                    sectionNewMatches.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                sectionNewMatches.setVisibility(View.GONE);
            }
        });
    }

    private void showNoConversations() {
        recyclerView.setVisibility(View.GONE);
        tvNoConversations.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("match_id", conversation.getMatchId());
        intent.putExtra("user_id", conversation.getUserId());
        intent.putExtra("user_name", conversation.getName());
        intent.putExtra("user_photo", conversation.getPrimaryPhoto());
        intent.putExtra("is_online", conversation.getIsOnline());
        intent.putExtra("last_seen", conversation.getLastSeen());
        startActivity(intent);
    }

    @Override
    public void onNewMatchClick(Match match) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("match_id", match.getMatchId());
        intent.putExtra("user_id", match.getUserId());
        intent.putExtra("user_name", match.getName());
        intent.putExtra("user_photo", match.getPrimaryPhoto());
        intent.putExtra("is_online", match.getIsOnline());
        intent.putExtra("last_seen", match.getLastSeen());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
