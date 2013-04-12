package olle.socialapp.domain;

public class Like {
	private String comment;
	private Person person;
	
	public Like(Person person, String comment) {
		this.person = person;
		this.comment = comment;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public Person getPerson() {
		return person;
	}
	
	public void setPerson(Person person) {
		this.person = person;
	}
	
	@Override
	public String toString() {
		return person.getName();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Like)) {
			return false;
		}
		Like other = (Like) obj;
		return this.person == null ? false : this.person.equals(other.person);
	}
	
	@Override
	public int hashCode() {
		if (this.person == null) {
			return 0;
		}
		return this.person.hashCode();
	}
}
