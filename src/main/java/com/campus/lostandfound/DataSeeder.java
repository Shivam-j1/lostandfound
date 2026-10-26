package com.campus.lostandfound;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Adds a few demo rows the first time the app runs on an empty database. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ItemRepository repository;

    public DataSeeder(ItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;
        repository.save(make("FOUND", "Black Casio Calculator", "Electronics", "Library 2nd Floor",
                "finder1@campus.edu", "What sticker is on the back?", "Marvel"));
        repository.save(make("FOUND", "Blue Water Bottle", "Bottles & Accessories", "Hostel Block B",
                "9876543210", "What name is written on the cap?", "Rahul"));
        repository.save(make("LOST", "Student ID Card", "ID & Keys", "Main Canteen",
                "owner@campus.edu", null, null));
    }

    private Item make(String type, String title, String cat, String loc, String contact, String q, String a) {
        Item i = new Item();
        i.setType(type); i.setTitle(title); i.setCategory(cat); i.setLocation(loc);
        i.setContactInfo(contact); i.setSecurityQuestion(q); i.setSecurityAnswer(a);
        return i;
    }
}
