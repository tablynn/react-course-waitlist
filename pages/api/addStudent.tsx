// fetch for informing the backend that a new student is attempting to join
export async function addToWaitlist(name: string, email: string, courseName: string) {
    const WaitlistUpdate_URL = "http://localhost:3231/addStudent?";
    const studentName = "studentName=" + name;
    const studentEmail = "email=" + email;
    const className = "className=" + courseName;
    await fetch(WaitlistUpdate_URL + studentName + "&" + studentEmail + "&" + className);
}