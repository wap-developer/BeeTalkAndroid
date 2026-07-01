package com.beeflirt.beetalk.db.mapper;

import com.beeflirt.beetalk.db.entity.ConversationEntity;
import com.beeflirt.beetalk.db.entity.MatchEntity;
import com.beeflirt.beetalk.db.entity.MessageEntity;
import com.beeflirt.beetalk.db.entity.PhotoEntity;
import com.beeflirt.beetalk.db.entity.ProfileEntity;
import com.beeflirt.beetalk.models.Conversation;
import com.beeflirt.beetalk.models.Match;
import com.beeflirt.beetalk.models.Message;
import com.beeflirt.beetalk.models.Photo;
import com.beeflirt.beetalk.models.Profile;

import java.util.ArrayList;
import java.util.List;

public class EntityMapper {

    public static MessageEntity toEntity(Message m) {
        MessageEntity e = new MessageEntity();
        e.setId(m.getId());
        e.setMatchId(m.getMatchId());
        e.setSenderId(m.getSenderId());
        e.setMessage(m.getMessage());
        e.setMessageType(m.getMessageType());
        e.setMediaUrl(m.getMediaUrl());
        e.setIsRead(m.getIsRead());
        e.setCreatedAt(m.getCreatedAt());
        return e;
    }

    public static Message toMessage(MessageEntity e) {
        Message m = new Message();
        m.setId(e.getId());
        m.setMatchId(e.getMatchId());
        m.setSenderId(e.getSenderId());
        m.setMessage(e.getMessage());
        m.setMessageType(e.getMessageType());
        m.setMediaUrl(e.getMediaUrl());
        m.setIsRead(e.getIsRead());
        m.setCreatedAt(e.getCreatedAt());
        return m;
    }

    public static List<MessageEntity> messagesToEntities(List<Message> messages) {
        List<MessageEntity> entities = new ArrayList<>();
        for (Message m : messages) {
            entities.add(toEntity(m));
        }
        return entities;
    }

    public static List<Message> entitiesToMessages(List<MessageEntity> entities) {
        List<Message> messages = new ArrayList<>();
        for (MessageEntity e : entities) {
            messages.add(toMessage(e));
        }
        return messages;
    }

    public static ConversationEntity toEntity(Conversation c) {
        ConversationEntity e = new ConversationEntity();
        e.setMatchId(c.getMatchId());
        e.setUserId(c.getUserId());
        e.setName(c.getName());
        e.setPrimaryPhoto(c.getPrimaryPhoto());
        e.setLastMessage(c.getLastMessage());
        e.setLastMessageTime(c.getLastMessageTime());
        e.setUnreadCount(c.getUnreadCount());
        e.setIsOnline(c.getIsOnline());
        e.setLastSeen(c.getLastSeen());
        return e;
    }

    public static Conversation toConversation(ConversationEntity e) {
        Conversation c = new Conversation();
        c.setMatchId(e.getMatchId());
        c.setUserId(e.getUserId());
        c.setName(e.getName());
        c.setPrimaryPhoto(e.getPrimaryPhoto());
        c.setLastMessage(e.getLastMessage());
        c.setLastMessageTime(e.getLastMessageTime());
        c.setUnreadCount(e.getUnreadCount());
        c.setIsOnline(e.getIsOnline());
        c.setLastSeen(e.getLastSeen());
        return c;
    }

    public static List<ConversationEntity> conversationsToEntities(List<Conversation> conversations) {
        List<ConversationEntity> entities = new ArrayList<>();
        for (Conversation c : conversations) {
            entities.add(toEntity(c));
        }
        return entities;
    }

    public static List<Conversation> entitiesToConversations(List<ConversationEntity> entities) {
        List<Conversation> conversations = new ArrayList<>();
        for (ConversationEntity e : entities) {
            conversations.add(toConversation(e));
        }
        return conversations;
    }

    public static MatchEntity toEntity(Match m) {
        MatchEntity e = new MatchEntity();
        e.setMatchId(m.getMatchId());
        e.setUserId(m.getUserId());
        e.setName(m.getName());
        e.setAge(m.getAge());
        e.setGender(m.getGender());
        e.setBio(m.getBio());
        e.setCity(m.getCity());
        e.setLatitude(m.getLatitude());
        e.setLongitude(m.getLongitude());
        e.setPrimaryPhoto(m.getPrimaryPhoto());
        e.setLastMessage(m.getLastMessage());
        e.setLastMessageTime(m.getLastMessageTime());
        e.setMatchedAt(m.getMatchedAt());
        e.setUnreadCount(m.getUnreadCount());
        e.setIsOnline(m.getIsOnline());
        e.setLastSeen(m.getLastSeen());
        return e;
    }

    public static Match toMatch(MatchEntity e) {
        Match m = new Match();
        m.setMatchId(e.getMatchId());
        m.setUserId(e.getUserId());
        m.setName(e.getName());
        m.setAge(e.getAge());
        m.setGender(e.getGender());
        m.setBio(e.getBio());
        m.setCity(e.getCity());
        m.setLatitude(e.getLatitude());
        m.setLongitude(e.getLongitude());
        m.setPrimaryPhoto(e.getPrimaryPhoto());
        m.setLastMessage(e.getLastMessage());
        m.setLastMessageTime(e.getLastMessageTime());
        m.setMatchedAt(e.getMatchedAt());
        m.setUnreadCount(e.getUnreadCount());
        m.setIsOnline(e.getIsOnline());
        m.setLastSeen(e.getLastSeen());
        return m;
    }

    public static List<MatchEntity> matchesToEntities(List<Match> matches) {
        List<MatchEntity> entities = new ArrayList<>();
        for (Match m : matches) {
            entities.add(toEntity(m));
        }
        return entities;
    }

    public static List<Match> entitiesToMatches(List<MatchEntity> entities) {
        List<Match> matches = new ArrayList<>();
        for (MatchEntity e : entities) {
            matches.add(toMatch(e));
        }
        return matches;
    }

    public static ProfileEntity toEntity(Profile p) {
        ProfileEntity e = new ProfileEntity();
        e.setUserId(p.getUserId());
        e.setName(p.getName());
        e.setGender(p.getGender());
        e.setBirthDate(p.getBirthDate());
        e.setAge(p.getAge());
        e.setBio(p.getBio());
        e.setUsername(p.getUsername());
        e.setHobbies(p.getHobbies());
        e.setDreams(p.getDreams());
        e.setLatitude(p.getLatitude());
        e.setLongitude(p.getLongitude());
        e.setCity(p.getCity());
        e.setCountry(p.getCountry());
        e.setDistance(p.getDistance());
        e.setPrimaryPhoto(p.getPrimaryPhoto());
        return e;
    }

    public static Profile toProfile(ProfileEntity e, List<PhotoEntity> photoEntities) {
        Profile p = new Profile();
        p.setUserId(e.getUserId());
        p.setName(e.getName());
        p.setGender(e.getGender());
        p.setBirthDate(e.getBirthDate());
        p.setAge(e.getAge());
        p.setBio(e.getBio());
        p.setUsername(e.getUsername());
        p.setHobbies(e.getHobbies());
        p.setDreams(e.getDreams());
        p.setLatitude(e.getLatitude());
        p.setLongitude(e.getLongitude());
        p.setCity(e.getCity());
        p.setCountry(e.getCountry());
        p.setDistance(e.getDistance());
        p.setPrimaryPhoto(e.getPrimaryPhoto());
        if (photoEntities != null) {
            p.setPhotos(entitiesToPhotos(photoEntities));
        }
        return p;
    }

    public static PhotoEntity toEntity(Photo ph) {
        PhotoEntity e = new PhotoEntity();
        e.setId(ph.getId());
        e.setUserId(ph.getUserId());
        e.setPhotoUrl(ph.getPhotoUrl());
        e.setIsPrimary(ph.getIsPrimary());
        e.setOrder(ph.getOrder());
        return e;
    }

    public static Photo toPhoto(PhotoEntity e) {
        Photo p = new Photo();
        p.setId(e.getId());
        p.setUserId(e.getUserId());
        p.setPhotoUrl(e.getPhotoUrl());
        p.setIsPrimary(e.getIsPrimary());
        p.setOrder(e.getOrder());
        return p;
    }

    public static List<PhotoEntity> photosToEntities(List<Photo> photos) {
        List<PhotoEntity> entities = new ArrayList<>();
        for (Photo p : photos) {
            entities.add(toEntity(p));
        }
        return entities;
    }

    public static List<Photo> entitiesToPhotos(List<PhotoEntity> entities) {
        List<Photo> photos = new ArrayList<>();
        for (PhotoEntity e : entities) {
            photos.add(toPhoto(e));
        }
        return photos;
    }
}
