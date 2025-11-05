package de.kilip.library;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.component.type.WritableBookContentComponent.MAX_PAGE_LENGTH;

public class BookUtils {

    public static ItemStack makeWritableBook(String contents, boolean use_huge_pages, int lenght_shorter_than_max_page_lenght) {
        List<RawFilteredPair<String>> pages = new ArrayList<>();
        StringBuilder pageContent = new StringBuilder();

        int max_page_lenght;

        if (use_huge_pages) max_page_lenght = MAX_PAGE_LENGTH;
        else max_page_lenght = lenght_shorter_than_max_page_lenght;

        for (char c : contents.toCharArray()) {
            pageContent.append(c);

            if (pageContent.length() >= max_page_lenght) {
                pages.add(RawFilteredPair.of(pageContent.toString()));
                pageContent.setLength(0);
            }
        }

        if (!pageContent.isEmpty()) {
            pages.add(RawFilteredPair.of(pageContent.toString()));
        }

        WritableBookContentComponent bookContent = new WritableBookContentComponent(pages);

        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponentTypes.WRITABLE_BOOK_CONTENT, bookContent);

        return book;
    }

    public static ArrayList<ItemStack> makeWritableBooks(String contents, boolean use_huge_pages,int lenght_shorter_than_max_page_lenght) {
        ArrayList<ItemStack> books = new ArrayList<>();

        int maxCharsPerBook = WritableBookContentComponent.MAX_PAGE_COUNT * MAX_PAGE_LENGTH;
        int totalLength = contents.length();

        for (int i = 0; i < totalLength; i += maxCharsPerBook) {
            int end = Math.min(i + maxCharsPerBook, totalLength);
            String part = contents.substring(i, end);
            books.add(makeWritableBook(part,use_huge_pages,lenght_shorter_than_max_page_lenght));
        }

        return books;
    }

    public static void giveItemStacks(ServerPlayerEntity player, ArrayList<ItemStack> itemStacks)
    {
        for(ItemStack itemStack : itemStacks){
            player.giveItemStack(itemStack);
        }
    }

}
