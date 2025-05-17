package daikon;

public class Invariant
{
    int id = -1;
    String desp = "";

    long success = 0L;
    long fail = 0L;
    int cooldown = 0;

    public boolean check() {return true;}

    public Invariant(int id, String desp) {
        this.id = id;
        this.desp = desp;
    }
}
