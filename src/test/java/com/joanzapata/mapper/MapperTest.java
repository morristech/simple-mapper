/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.joanzapata.mapper;

import com.joanzapata.mapper.model.*;
import com.joanzapata.mapper.model.entry.AddressEntry;
import com.joanzapata.mapper.model.entry.AddressEntryDTO;
import com.joanzapata.mapper.model.entry.PhoneEntry;
import com.joanzapata.mapper.model.entry.PhoneEntryDTO;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MapperTest {

    private final Logger logger = LoggerFactory.getLogger(MapperTest.class);

    @Test
    public void singleObject() {
        logger.info("singleObject");
        Mapper mapper = new Mapper();
        Book book = new Book(5L, "Book");
        BookDTO bookDTO = mapper.map(book, BookDTO.class);
        assertEquals(book.getId(), bookDTO.getId());
        assertEquals(book.getName(), bookDTO.getName());
    }

    @Test
    public void singleObjectList() {
        logger.info("singleObjectList");
        Mapper mapper = new Mapper();
        Book b1 = new Book(1L, "Book1");
        Book b2 = new Book(2L, "Book2");
        Book b3 = new Book(3L, "Book3");
        List<Book> bookList = Arrays.asList(b1, b2, b3);
        List<BookDTO> bookListDTO = mapper.map(bookList, BookDTO.class);
        assertEquals(new Long(1), bookListDTO.get(0).getId());
        assertEquals(new Long(2), bookListDTO.get(1).getId());
        assertEquals(new Long(3), bookListDTO.get(2).getId());
    }

    @Test
    public void objectWithCyclicDependencies() {
        logger.info("objectWithCyclicDependencies");

        Book book = new Book(0L, "Book");
        BookEntry entry1 = new BookEntry(1, book);
        BookEntry entry2 = new BookEntry(2, book);
        book.setEntries(Arrays.asList(entry1, entry2));

        Mapper mapper = new Mapper();
        BookDTO bookDTO = mapper.map(book, BookDTO.class);

        BookEntryDTO entry1DTO = bookDTO.getEntries().get(0);
        BookEntryDTO entry2DTO = bookDTO.getEntries().get(1);

        assertEquals(entry1DTO.getId(), entry1.getId());
        assertEquals(entry2DTO.getId(), entry2.getId());
        assertEquals(bookDTO, entry1DTO.getBookDTO());
        assertEquals(bookDTO, entry2DTO.getBookDTO());
    }

    @Test
    public void inheritance() {
        logger.info("inheritance");
        Book book = createTestBook();

        Mapper mapper = new Mapper()
                .mapping(AddressEntry.class, AddressEntryDTO.class)
                .mapping(PhoneEntry.class, PhoneEntryDTO.class);

        BookDTO bookDTO = mapper.map(book, BookDTO.class);

        assertEquals(2, bookDTO.getEntries().size());

        BookEntryDTO phoneEntryDTO = bookDTO.getEntries().get(0);
        BookEntryDTO addressEntryDTO = bookDTO.getEntries().get(1);

        Assert.assertTrue(phoneEntryDTO instanceof PhoneEntryDTO);
        Assert.assertTrue(addressEntryDTO instanceof AddressEntryDTO);

        assertEquals(
                ((PhoneEntry) book.getEntries().get(0)).getPhoneNumber(),
                ((PhoneEntryDTO) phoneEntryDTO).getPhoneNumber());
        assertEquals(
                ((AddressEntry) book.getEntries().get(1)).getCity(),
                ((AddressEntryDTO) addressEntryDTO).getCity());

        assertEquals(bookDTO, phoneEntryDTO.getBookDTO());
        assertEquals(bookDTO, addressEntryDTO.getBookDTO());
    }

    @Test(expected = StrictModeException.class)
    public void throwExceptionIfPropertyNotFoundInSource() {
        logger.info("throwExceptionIfPropertyNotFoundInSource");
        new Mapper()
                .strictMode(true)
                .map(new A(), B.class);
    }

    @Test
    public void bidirectionalMappings() {
        logger.info("bidirectionalMappings");
        Book book = createTestBook();

        Mapper mapper = new Mapper()
                .biMapping(AddressEntry.class, AddressEntryDTO.class)
                .biMapping(PhoneEntry.class, PhoneEntryDTO.class)
                .strictMode(true);

        BookDTO bookDTO = mapper.map(book, BookDTO.class);
        Book newBook = mapper.map(bookDTO, Book.class);

        assertEquals(book.getId(), newBook.getId());
        assertEquals(book.getName(), newBook.getName());

        assertEquals(
                book.getEntries().get(0).getId(),
                newBook.getEntries().get(0).getId());
        assertEquals(
                book.getEntries().get(1).getId(),
                newBook.getEntries().get(1).getId());
    }

    @Test
    public void nameVariations() {
        logger.info("nameVariations");
        NameVariationTest in = new NameVariationTest();
        NameVariationTestDTO out = new Mapper().map(in, NameVariationTestDTO.class);
        Assert.assertEquals(in.getTest(), out.getTestDTO());
        Assert.assertEquals(in.getTestOther(), out.getTestOtherDTO());
    }

    @Test
    public void testHook() {
        logger.info("testHook");
        Mapper mapper = new Mapper()
                .hook(new Hook<Book, BookDTO>() {
                    @Override
                    public void extraMapping(Book from, BookDTO to) {
                        to.setName("ItWorks.");
                    }
                });
        Book testBook = createTestBook();
        BookDTO out = mapper.map(testBook, BookDTO.class);
        assertEquals("ItWorks.", out.getName());
    }

    @Test
    public void testHookWithInheritance() {
        logger.info("testHookWithInheritance");
        Mapper mapper = new Mapper()
                .hook(new Hook<BookEntry, BookEntryDTO>() {
                    @Override
                    public void extraMapping(BookEntry from, BookEntryDTO to) {
                        to.setId(1337);
                    }
                });
        Book testBook = createTestBook();
        BookDTO out = mapper.map(testBook, BookDTO.class);
        assertEquals(1337, out.getEntries().get(0).getId());
        assertEquals(1337, out.getEntries().get(1).getId());
    }

    @Test
    public void testMapMapping() {
        logger.info("testMapMapping");
        Mapper mapper = new Mapper();
        Book testBook = createTestBook();
        BookDTO out = mapper.map(testBook, BookDTO.class);
        assertEquals(1L, out.getEntriesById().get(1L).getId());
        assertEquals(2L, out.getEntriesById().get(2L).getId());
    }

    @Test
    public void testNull() {
        logger.info("testNull");
        Mapper mapper = new Mapper();
        assertNull(mapper.map(null, BookDTO.class));
    }

    @Test
    public void testDirectMapMapping() {
        logger.info("testDirectMapMapping");
        Mapper mapper = new Mapper()
                .biMapping(AddressEntry.class, AddressEntryDTO.class)
                .biMapping(PhoneEntry.class, PhoneEntryDTO.class)
                .strictMode(true);

        Map<Long, Book> in = new HashMap<Long, Book>();
        in.put(1L, createTestBook(1L));
        in.put(2L, createTestBook(2L));

        Map<Long, BookDTO> out = mapper.map(in, Long.class, BookDTO.class);
        assertEquals(1l, (long) out.get(1L).getId());
        assertEquals(2l, (long) out.get(2L).getId());
    }

    @Test(expected = StrictModeException.class)
    public void testIncompatibleTypes() {
        logger.info("testIncompatibleTypes");
        final Mapper mapper = new Mapper().strictMode(true);
        mapper.map(1L, Byte.class);
    }

    @Test(expected = StrictModeException.class)
    public void testDirectIncompatibleTypes() {
        logger.info("testDirectIncompatibleTypes");
        Map<Long, String> input = new HashMap<Long, String>();
        input.put(1L, "1");
        Map<Long, BookDTO> incompatibleOutput = new Mapper().strictMode(true).map(input, Long.class, BookDTO.class);
    }

    @Test
    public void testEnumDirect() {
        Mapper mapper = new Mapper();
        assertEquals(EnumSourceDTO.A, mapper.map(EnumSource.A, EnumSourceDTO.class));
        assertEquals(EnumSourceDTO.B, mapper.map(EnumSource.B, EnumSourceDTO.class));
        assertEquals(EnumSourceDTO.C, mapper.map(EnumSource.C, EnumSourceDTO.class));
    }

    @Test
    public void testEnumIndirect() {
        ModelWithEnum input = new ModelWithEnum();
        Mapper mapper = new Mapper();
        final ModelWithEnumDTO output = mapper.map(input, ModelWithEnumDTO.class);
        assertEquals(ModelWithEnumDTO.MyEnumDTO.A, output.getMyEnumsDTO().get(0));
    }

    private Book createTestBook() {
        return createTestBook(0L);
    }

    private Book createTestBook(long id) {
        Book book = new Book(id, "Book");

        PhoneEntry entry1 = new PhoneEntry();
        entry1.setBook(book);
        entry1.setId(1);
        entry1.setPhoneNumber("123456789");

        AddressEntry entry2 = new AddressEntry();
        entry2.setBook(book);
        entry2.setId(2);
        entry2.setCity("Paris");
        entry2.setCountry("France");

        book.setEntries(Arrays.asList(entry1, entry2));

        Map<Long, BookEntry> map = new HashMap<Long, BookEntry>();
        map.put(entry1.getId(), entry1);
        map.put(entry2.getId(), entry2);
        book.setEntriesById(map);

        return book;
    }

    public static enum EnumSource {
        A, B, C
    }

    public static enum EnumSourceDTO {
        A, B, C
    }

    public static class A {
    }

    public static class B {
        public void setName(String name) {
        }
    }

    public static class NameVariationTest {
        String test = "Test", testOther = "TestOther";

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public String getTestOther() {
            return testOther;
        }

        public void setTestOther(String testOther) {
            this.testOther = testOther;
        }
    }

    public static class NameVariationTestDTO {
        String testDTO, testOtherDTO;

        public String getTestDTO() {
            return testDTO;
        }

        public void setTestDTO(String testDTO) {
            this.testDTO = testDTO;
        }

        public String getTestOtherDTO() {
            return testOtherDTO;
        }

        public void setTestOtherDTO(String testOtherDTO) {
            this.testOtherDTO = testOtherDTO;
        }
    }
}
