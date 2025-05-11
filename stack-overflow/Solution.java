public class StackOverflow {}

public class User {

    private final long id;
    private final String name;
    private final Reputation reputation;

    public User(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Question postQuestion(String title, String body) {
        Question question = new Question(this, title, body);
        QuestionManager.getInstance().createQuestion(question);
        return question;
    }

    public Answer postAnswer(Question question, String answerBody) {
        final var answer = new TextAnswer(answerBody);
        question.addAnswer(answer);
        return answer;
    }

    public Comment postComment(Commentable commentable, String commentBody) {
        final var comment = new Comment(commentBody);
        commentable.addComment(comment);
        return comment;
    }

    public void postVote(Votable votable, Vote vote) {
        votable.vote(vote);
    }

    public boolean acceptAnswer(Question question, Answer answer) {
        question.acceptAnswer(this, answer);
    }

    public Reputation increaseReputation(int points) {
        reputation.increase(points);
        return reputation;
    }

    public Reputation decreaseReputation(int points) {
        reputation.decrease(points);
        return reputation;
    }
}

public class QuestionManager {

    private static class HOLDER {

        private static final QuestionManager INSTANCE = new QuestionManager();
    }

    public static QuestionManager getInstance() {
        return HOLDER.INSTANCE;
    }

    private final ConcurrentMap<Long, Question> questions;

    private QuestionManager() {
        this.questions = new ConcurrentHashMap<>();
    }

    public Question createQuestion(Question question) {
        final var id = newId(question);
        question.setId(id);
        questions.put(question.getId(), question);
        return question;
    }

    private long newId(Question question) {
        return questions.size() + 1L;
    }
}

public interface Commentable {
    public void addComment(Comment comment);
}

public interface Votable {
    public void vote(Vote vote);
}

public class Question implements Commentable, Votable {

    private Long id;
    private final User author;
    private String title;
    private String body;
    private int score;
    private final Set<Tag> tags;
    private final List<Answer> answers;
    private final List<Comment> comments;
    private Optional<AcceptedAnswer> acceptedAnswer;

    public Question(Long id, User author, String title, String body) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.body = body;
        this.score = 0;
        this.tags = new ConcurrentSkipListSet<>();
        this.answers = new CopyOnWriteArrayList<>();
        this.comments = new CopyOnWriteArrayList<>();
        this.acceptedAnswer = Optional.empty();
    }

    public Question(User author, String title, String body) {
        this(null, author, title, body);
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public void vote(Vote vote) {
        this.score += vote.getValue();
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void addAnswer(Answer answer) {
        this.answers.add(answer);
    }

    @Override
    public void addComment(Comment comment) {
        this.comments.add(comment);
    }

    public boolean acceptAnswer(User user, Answer answer) {
        if (user.equals(user) && answers.contains(answer)) {
            this.acceptedAnswer = Optional.of(new AcceptedAnswer(answer));
            return true;
        }
        return false;
    }
}

public abstract class Answer implements Commentable, Votable {

    private int score;
    private final List<Comment> comments;

    protected Answer() {
        this.score = 0;
        this.comments = new CopyOnWriteArrayList<>();
    }

    public abstract boolean isAccepted();

    @Override
    public void addComment(Comment comment) {
        this.comments.add(comment);
    }

    @Override
    public void vote(Vote vote) {
        this.score += vote.getValue();
    }

    public int getScore() {
        return score;
    }
}

public class TextAnswer extends Answer {

    private final String text;

    public TextAnswer(String text) {
        this.text = text;
    }

    @Override
    public boolean isAccepted() {
        return false;
    }
}

public class AcceptedAnswer extends Answer {

    private Answer answer;

    public AcceptedAnswer(Answer answer) {
        this.answer = answer;
    }

    @Override
    public boolean isAccepted() {
        return true;
    }
}

public class Vote {

    private int value;

    public Vote(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

public class Comment {

    private String text;

    public Comment(String text) {
        this.text = text;
    }
}

public class Tag {

    final String name;
}

public class Reputation {

    private AtomicInteger points;

    public Reputation() {
        this.points = new AtomicInteger(0);
    }

    public Reputation(int points) {
        this.points = new AtomicInteger(points);
    }

    public void increase(int points) {
        this.points.addAndGet(points);
    }

    public void decrease(int points) {
        this.points.addAndGet(-points);
    }

    public int getPoints() {
        return points.get();
    }
}
