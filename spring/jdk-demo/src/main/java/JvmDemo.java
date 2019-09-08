public class JvmDemo {

    public static void main(String[] args) {
        Person person = new Person();
        person.setName("zhangsan");
        change(person);
        System.out.println(person.getName());

    }

    public static void change(Person change){
        change = null;
    }
}
