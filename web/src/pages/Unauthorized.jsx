const Unauthorized = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white rounded-lg shadow-lg p-8 max-w-md text-center">
        <div className="text-6xl mb-4">🚫</div>
        <h1 className="text-2xl font-bold mb-4">Access Denied</h1>
        <p className="text-gray-600 mb-6">
          You don't have permission to access this page. Please contact the administrator if you believe this is an error.
        </p>
        <a 
          href="/login" 
          className="inline-block bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg"
        >
          Go to Login
        </a>
      </div>
    </div>
  );
};

export default Unauthorized;
