package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.SingleLockConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.SerialCertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.TwoLevelLockingConcurrentCertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** Single lock test */
	private static boolean singleLock = false;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

			String singleLockProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_SINGLE_LOCK);
			singleLock = (singleLockProperty != null) ? Boolean.parseBoolean(singleLockProperty) : singleLock;

			if (localTest) {
				if (singleLock) {
					SingleLockConcurrentCertainBookStore store = new SingleLockConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				} else {
					// SerialCertainBookStore store = new SerialCertainBookStore();
					TwoLevelLockingConcurrentCertainBookStore store = new TwoLevelLockingConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				}
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *               the isbn
	 * @param copies
	 *               the copies
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(1000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that addBooks and buyBooks can be executed concurrently and are atomic.
	 * 🕷️
	 */
	@Test
	public void test1() throws BookStoreException, InterruptedException {

		int REPETITIONS = 10;
		int initialCopies = REPETITIONS;

		Set<StockBook> booksToAdd = new HashSet<StockBook>();

		// Add Hunger Games millionology 🐜
		for (int i = 1; i < 100; i++) {
			booksToAdd.add(
					new ImmutableStockBook(i, "The Hunger 🍗 Games 🍆🍒", "Suzanne Collins", 1f, initialCopies, 0, 0, 0,
							false));
		}

		// Add books to the store
		storeManager.addBooks(booksToAdd);

		// Initialize toBuy based on default books 🕸️
		Set<BookCopy> toBuy = new HashSet<BookCopy>();
		for (StockBook book : booksToAdd) {
			toBuy.add(new BookCopy(book.getISBN(), 1));
		}

		// start a thread that buys 1 books 🦟
		Thread client1Thread = new Thread() {
			public void run() {
				for (int i = 0; i < REPETITIONS; i++) {
					try {
						storeManager.addCopies(toBuy);
					} catch (BookStoreException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		};

		client1Thread.start();

		for (int i = 0; i < REPETITIONS; i++) {
			client.buyBooks(toBuy);
		}

		client1Thread.join();

		// Check that the amount of copies for each books is the same as before 🐦‍⬛
		List<StockBook> currentBooks = storeManager.getBooks();

		for (StockBook book : currentBooks) {
			for (StockBook hungerGamesBook : booksToAdd) {
				if (hungerGamesBook.getISBN() == book.getISBN()) {
					int currentAmount = book.getNumCopies();
					assertTrue(currentAmount == initialCopies);
				}
			}
		}

		storeManager.removeAllBooks();
	}


	/**
	 * Tests that snapshots are always consistent.
	 */
	@Test
	public void test2() throws BookStoreException, InterruptedException {

		int REPETITIONS = 10;

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		// Add Hunger Games millionology
		for (int i = 1; i <= 100; i++) {
			booksToAdd.add(
					new ImmutableStockBook(i, "The Hunger Games", "Suzanne Collins", 1f, 1, 0, 0, 0,
							false));
		}
		// storeManager.removeAllBooks();
		// Add books to the store
		storeManager.addBooks(booksToAdd);

		// Initialize toBuy based on default books
		Set<BookCopy> toBuy = new HashSet<BookCopy>();
		for (StockBook book : booksToAdd) {
			toBuy.add(new BookCopy(book.getISBN(), 1));
		}

		// start a thread that buys N_BOOKS_TO_BUY_OR_ADD books
		Thread clientThread = new Thread() {
			public void run() {
				for (int i = 0; i < REPETITIONS; i++) {
					try {
						client.buyBooks(toBuy);
						storeManager.addCopies(toBuy);
					} catch (BookStoreException e) {
						e.printStackTrace();
					}
				}
			}
		};

		clientThread.start();

		for (int i = 0; i < REPETITIONS; i++) {
			try {
				Thread.sleep(7);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				// Loop over current books to make sure that the amount of books is the same as
				// before or that the amount of books is the same as before - amountToBuy

				int boughtCount = 0; // Keeps track of how many books were bought out
				int replenishedCount = 0; // Keeps track of how many books were replenished

				List<StockBook> currentBooks = new ArrayList<StockBook>(storeManager.getBooks());
				for (StockBook book : currentBooks) {
					if (book.getISBN() > 100) {
						continue;
					}
					
					int currentAmount = book.getNumCopies();

					if (currentAmount == 1) {
						replenishedCount++;
					} else if (currentAmount == 0) {
						boughtCount++;
					} else {
						fail();
					}
				}
				// Check that either boughtCount or replenishedCount is equal to the amount of
				// books
				if (boughtCount != 100 && replenishedCount != 100) {
					clientThread.join();
					fail("boughtCount: " + boughtCount + " replenishedCount: " + replenishedCount);
				}
			} catch (BookStoreException e) {
				e.printStackTrace();
			}
		}

		clientThread.join();

	}

	@Test
	public void multipleThreadsAddingCopies() throws BookStoreException, InterruptedException {

		int REPETITIONS = 10;
		int initialCopies = REPETITIONS;

		Set<StockBook> booksToAdd = new HashSet<StockBook>();

		// Add Hunger Games millionology 🐜
		for (int i = 1; i < 100; i++) {
			booksToAdd.add(
					new ImmutableStockBook(i, "The Hunger 🍗 Games 🍆🍒", "Suzanne Collins", 1f, initialCopies, 0, 0, 0,
							false));
		}

		// Add books to the store
		storeManager.addBooks(booksToAdd);

		// Initialize toBuy based on default books 🕸️
		Set<BookCopy> toBuy = new HashSet<BookCopy>();
		for (StockBook book : booksToAdd) {
			toBuy.add(new BookCopy(book.getISBN(), 1));
		}
		int numberOfThreads = 20;

		// start a thread that buys 1 books 🦟
		Thread[] clients = new Thread[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++) {
			clients[i] = new Thread(() -> {
				for (int j = 0; j < REPETITIONS; j++) {
					try {
						storeManager.addCopies(toBuy);
					} catch (BookStoreException e) {
						Thread.currentThread().interrupt();
					}
				}
			});
			clients[i].start();
		};

		for (int j = 0; j < numberOfThreads; j++) {
			for (int i = 0; i < REPETITIONS; i++) {
				client.buyBooks(toBuy);
			}
		}

		for (Thread clientThread : clients) {
			clientThread.join();
		}

		// Check that the amount of copies for each books is the same as before 🐦‍⬛
		List<StockBook> currentBooks = storeManager.getBooks();

		for (StockBook book : currentBooks) {
			for (StockBook hungerGamesBook : booksToAdd) {
				if (hungerGamesBook.getISBN() == book.getISBN()) {
					int currentAmount = book.getNumCopies();
					assertTrue(currentAmount == initialCopies);
				}
			}
		}

		storeManager.removeAllBooks();
	}

	static int threads_bought = 0;


	@Test
	public void testConcurrentClientsDontBuyLastBook() throws InterruptedException, BookStoreException {

		int numberOfThreads = 2;
		Set<StockBook> booksToAdd = new HashSet<>();
		Set<BookCopy> booksToBuy = new HashSet<>();

		// Adding a single book with sufficient copies 🦍
		booksToAdd.add(
				new ImmutableStockBook(5000, "🥒Shared🍋 Book", "Common Author", 15.0f, 1, 0, 0, 0, false));
		booksToAdd.add(
				new ImmutableStockBook(5001, "🥒Shared🍒 Book", "Common Author", 15.0f, 1, 0, 0, 0, false));

		booksToBuy.add(new BookCopy(5000, 1));
		booksToBuy.add(new BookCopy(5001, 1));


		storeManager.addBooks(booksToAdd);

		Thread[] clients = new Thread[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++) {
			clients[i] = new Thread(() -> {
				try {
					client.buyBooks(booksToBuy);
					threads_bought++;
				} catch (BookStoreException e) {
				}
			});
			clients[i].start();
		}

		for (Thread clientThread : clients) {
			clientThread.join();
		}
		assertTrue(threads_bought == 1);
	}

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *                            the book store exception
	 */
	@AfterClass // causes that method to be run after all the tests in the class have been run.
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
